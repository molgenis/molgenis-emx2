package org.molgenis.emx2.harvester;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.harvester.jsonld.JsonLdFrameGenerator;
import org.molgenis.emx2.harvester.jsonld.JsonLdFramer;
import org.molgenis.emx2.sql.TestDatabaseFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;

public class JsonldTransformerTest {

  @Test
  void jsonLd() throws IOException {
    Repository repository = new SailRepository(new MemoryStore());

    try (RepositoryConnection conn = repository.getConnection();
         InputStream stream = readTtl("pet-store.ttl");
         OutputStream out = new ByteArrayOutputStream()) {

      RDFWriter writer = Rio.createWriter(RDFFormat.JSONLD, out);
      writer.startRDF();

      Rio.parse(stream, RDFFormat.TURTLE)
              .forEach(
                      statement -> {
                        try {
                          writer.handleStatement(statement);
                        } catch (Exception e) {
                          e.printStackTrace();
                        }
                      });

      // End the JSON-LD document
      writer.endRDF();

      String jsonld = out.toString();

      System.out.println(jsonld);
    }

    //    try {
    //      StringWriter writer = new StringWriter();
    //      RDFWriter rdfWriter = Rio.createWriter(RDFFormat.JSONLD, writer);
    //      WriterConfig config = rdfWriter.getWriterConfig();
    //      config.set(JSONLDSettings.JSONLD_MODE, JSONLDMode.EXPAND);
    //      config.set(JSONLDSettings.COMPACT_ARRAYS, true);
    //      rdfWriter.startRDF();
    //      for (org.eclipse.rdf4j.model.Statement statement : model) {
    //        rdfWriter.handleStatement(statement);
    //      }
    //      rdfWriter.endRDF();
    //      return writer.toString();
    //    }
  }

  private record FrameResult(JsonNode frame, JsonNode framedJson) {}

  private static final String DCAT_CATALOG = "dcat:Catalog";
  private static final String DCAT_DATASET = "dcat:Dataset";
  private static final String JSON_LD_VALUE = "@value";
  private static final String JSON_LD_TYPE = "@type";
  private static final String JSON_LD_GRAPH = "@graph";
  private static final String JSON_LD_ID = "@id";

  @Test
  void jsonLdFrames() throws IOException {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.getSchema("pet store");
    Model rdfModel = Rio.parse(readTtl("pet-store.ttl"), RDFFormat.TURTLE);

    JsonLdFrameGenerator frameGenerator = new JsonLdFrameGenerator();
    JsonNode frame = frameGenerator.generate(schema.getMetadata());

    JsonLdFramer framer = new JsonLdFramer();
    JsonNode framedJson = framer.frame(rdfModel, frame);

    FrameResult frameResult = new FrameResult(frame, framedJson);

    Set<String> targetColumns = resolveTargetColumns(frameResult.frame, schema);
    Map<String, String> reverseContext =
            buildReverseContext(frameResult.frame.get("@context"), targetColumns);
    List<JsonNode> graphItems = extractGraphItems(frameResult.framedJson);
    List<Row> rows = collectResourceRows(graphItems, reverseContext);
    rows.forEach(row -> System.out.println(row.toString()));
  }

  private Set<String> resolveTargetColumns(JsonNode frame, Schema schema) {
    String tableName = findTableNameForType(frame, DCAT_CATALOG);
    if (tableName == null) {
      tableName = findTableNameForType(frame, DCAT_DATASET);
    }
    if (tableName == null) {
      tableName = "Resources";
    }
    if (schema == null) {
      return Set.of();
    }
    Table table = schema.getTable(tableName);
    if (table == null) {
      return Set.of();
    }
    return table.getMetadata().getColumns().stream()
            .filter(col -> col.getKey() != 1)
            .map(Column::getIdentifier)
            .collect(Collectors.toSet());
  }

  String findTableNameForType(JsonNode frame, String dcatType) {
    JsonNode context = frame.get("@context");
    if (context == null || !context.isObject()) {
      return null;
    }
    for (Iterator<Map.Entry<String, JsonNode>> it = context.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      JsonNode value = entry.getValue();
      if (value.isObject() && dcatType.equals(value.path(JSON_LD_TYPE).asText(null))) {
        return entry.getKey();
      }
    }
    return null;
  }

  List<JsonNode> extractGraphItems(JsonNode framedJson) {
    String jsonLdType = "@type";
    String jsonLdGraph = "@graph";

    List<JsonNode> items = new ArrayList<>();
    if (framedJson.has(jsonLdGraph) && framedJson.get(jsonLdGraph).isArray()) {
      for (JsonNode item : framedJson.get(jsonLdGraph)) {
        items.add(item);
      }
    } else if (framedJson.has(jsonLdType)) {
      items.add(framedJson);
    }
    return items;
  }

  Map<String, String> buildReverseContext(JsonNode context, Set<String> allowedColumns) {
    Map<String, String> reverseMap = new HashMap<>();
    if (context == null || !context.isObject()) {
      return reverseMap;
    }
    for (Iterator<Map.Entry<String, JsonNode>> it = context.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      String columnName = entry.getKey();
      JsonNode value = entry.getValue();
      if (value.isObject() && value.has(JSON_LD_ID) && !isReferenceEntry(value)) {
        if (allowedColumns != null && !allowedColumns.contains(columnName)) {
          continue;
        }
        String predicateIri = value.get(JSON_LD_ID).asText();
        reverseMap.putIfAbsent(predicateIri, columnName);
      }
    }
    return reverseMap;
  }

  boolean isReferenceEntry(JsonNode contextEntry) {
    JsonNode typeNode = contextEntry.get(JSON_LD_TYPE);
    return typeNode != null && JSON_LD_ID.equals(typeNode.asText());
  }

  private List<Row> collectResourceRows(List<JsonNode> items, Map<String, String> reverseContext) {
    List<Row> resourceRows = new ArrayList<>();
    for (JsonNode item : items) {
      String itemType = extractType(item);
      if (isDcatResource(itemType)) {
        try {
          Row row = nodeToRow(item, itemType, reverseContext);
          resourceRows.add(row);
        } catch (Exception e) {
          String id = item.has(JSON_LD_ID) ? item.get(JSON_LD_ID).asText() : "unknown";
        }
      }
    }
    return resourceRows;
  }

  String extractType(JsonNode item) {
    JsonNode typeNode = item.get(JSON_LD_TYPE);
    if (typeNode == null) {
      return null;
    }
    if (typeNode.isTextual()) {
      return typeNode.asText();
    }
    if (typeNode.isArray() && typeNode.size() > 0) {
      return typeNode.get(0).asText();
    }
    return null;
  }

  boolean isDcatResource(String itemType) {
    return DCAT_CATALOG.equals(itemType)
            || DCAT_DATASET.equals(itemType)
            || "dcat:DataService".equals(itemType);
  }

  Row nodeToRow(JsonNode item, String itemType, Map<String, String> reverseContext) {
    Row row = new Row();
    item.fields()
            .forEachRemaining(
                    entry -> {
                      String rawKey = entry.getKey();
                      JsonNode value = entry.getValue();
                      if (rawKey.startsWith("@") || value.isNull()) {
                        return;
                      }
                      String columnName = reverseContext.get(rawKey);
                      if (columnName != null) {
                        setRowField(row, columnName, value);
                      }
                    });

    if (!row.getColumnNames().contains("type")) {
      String inferredType = inferTypeFromDcatType(itemType);
      if (inferredType != null) {
        row.set("type", new String[] {inferredType});
      }
    }

    if (!row.getColumnNames().contains("id") && item.has(JSON_LD_ID)) {
      String extractedId = extractIdFromIri(item.get(JSON_LD_ID).asText(), itemType);
      if (extractedId != null) {
        row.set("id", extractedId);
      }
    }

    return row;
  }

  String extractIdFromIri(String iri, String itemType) {
    if (iri == null || iri.isBlank()) {
      return null;
    }
    String[] pathParts = iri.split("/");
    String lastSegment = pathParts[pathParts.length - 1];
    if (lastSegment.contains("=")) {
      String[] keyValue = lastSegment.split("=", 2);
      if (keyValue.length == 2) {
        return keyValue[1];
      }
    }
    String prefix;
    if (DCAT_CATALOG.equals(itemType)) {
      prefix = "catalog-";
    } else if (DCAT_DATASET.equals(itemType)) {
      prefix = "dataset-";
    } else {
      prefix = "";
    }
    return prefix + lastSegment;
  }

  void setRowField(Row row, String columnName, JsonNode value) {
    if (value.isTextual()) {
      row.set(columnName, value.asText());
    } else if (value.isObject()) {
      if (value.has(JSON_LD_VALUE)) {
        row.set(columnName, value.get(JSON_LD_VALUE).asText());
      } else if (value.has(JSON_LD_ID)) {
        row.set(columnName, value.get(JSON_LD_ID).asText());
      }
    } else if (value.isArray()) {
      List<String> elements = new ArrayList<>();
      for (JsonNode element : value) {
        if (element.isTextual()) {
          elements.add(element.asText());
        } else if (element.has(JSON_LD_VALUE)) {
          elements.add(element.get(JSON_LD_VALUE).asText());
        } else if (element.has(JSON_LD_ID)) {
          elements.add(element.get(JSON_LD_ID).asText());
        }
      }
      if (!elements.isEmpty()) {
        row.set(columnName, elements.toArray(new String[0]));
      }
    }
  }

  String inferTypeFromDcatType(String dcatType) {
    if (DCAT_CATALOG.equals(dcatType)) {
      return "Catalogue";
    }
    if (DCAT_DATASET.equals(dcatType)) {
      return "Cohort study";
    }
    return null;
  }

  private InputStream readTtl(String path) {
    return TransformerTest.class.getResourceAsStream(path);
  }
}
