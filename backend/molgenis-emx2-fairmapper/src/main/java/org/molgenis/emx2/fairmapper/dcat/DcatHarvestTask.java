package org.molgenis.emx2.fairmapper.dcat;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFrameGenerator;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFramer;
import org.molgenis.emx2.fairmapper.rdf.RdfFetcher;
import org.molgenis.emx2.tasks.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DcatHarvestTask extends Task {
  private static final Logger log = LoggerFactory.getLogger(DcatHarvestTask.class);
  private static final int MAX_DEPTH = 2;
  private static final int MAX_CALLS = 100;

  private final Schema schema;
  private final String catalogUrl;
  private final String rdfContent;

  public DcatHarvestTask(Schema schema, String catalogUrl) {
    super("DCAT harvest: " + catalogUrl);
    this.schema = schema;
    this.catalogUrl = catalogUrl;
    this.rdfContent = null;
  }

  public DcatHarvestTask(Schema schema, String label, String rdfContent) {
    super("DCAT harvest: " + label);
    this.schema = schema;
    this.catalogUrl = null;
    this.rdfContent = rdfContent;
  }

  @Override
  public void run() {
    try {
      this.start();
      HarvestReport report;
      if (rdfContent != null) {
        report = harvestRdf(rdfContent);
      } else {
        report = harvestUrl(catalogUrl);
      }
      if (report.hasErrors()) {
        for (String error : report.getErrors()) {
          this.addSubTask("Import error").setError(error);
        }
        this.setError("Harvest completed with errors");
      } else {
        this.complete("Imported " + report.getResourcesImported() + " resources");
      }
    } catch (Exception e) {
      this.setError("Harvest failed: " + e.getMessage());
    }
  }

  private HarvestReport harvestUrl(String url) {
    HarvestReport report = new HarvestReport();
    Task currentStep = null;
    try {
      log.info("Starting DCAT harvest from: {}", url);

      JsonNode frame = generateFrame();

      currentStep = this.addSubTask("Fetching RDF from " + url);
      currentStep.start();
      RdfFetcher fetcher = new RdfFetcher();
      Model rdfModel = fetcher.fetchRecursively(url, MAX_DEPTH, MAX_CALLS);
      log.info("Fetched {} RDF statements", rdfModel.size());
      currentStep.complete("Fetched " + rdfModel.size() + " RDF statements");

      frameTransformImport(frame, rdfModel, report);
    } catch (IOException e) {
      String msg = "Harvest failed: " + e.getMessage();
      log.error(msg, e);
      if (currentStep != null) currentStep.setError(msg);
      report.addError(msg);
    } catch (DcatHarvestException e) {
      String msg = "Harvest error: " + e.getMessage();
      log.error(msg, e);
      if (currentStep != null) currentStep.setError(msg);
      report.addError(msg);
    }
    return report;
  }

  public HarvestReport harvestRdf(String rdfTurtle) {
    HarvestReport report = new HarvestReport();
    Task currentStep = null;
    try {
      log.info("Starting DCAT harvest from pasted RDF ({} chars)", rdfTurtle.length());

      JsonNode frame = generateFrame();

      currentStep = this.addSubTask("Parsing RDF");
      currentStep.start();
      Model rdfModel = parseTurtle(rdfTurtle);
      log.info("Parsed {} RDF statements", rdfModel.size());
      currentStep.complete("Parsed " + rdfModel.size() + " RDF statements");

      frameTransformImport(frame, rdfModel, report);
    } catch (IOException e) {
      String msg = "Harvest failed: " + e.getMessage();
      log.error(msg, e);
      if (currentStep != null) currentStep.setError(msg);
      report.addError(msg);
    } catch (DcatHarvestException e) {
      String msg = "Harvest error: " + e.getMessage();
      log.error(msg, e);
      if (currentStep != null) currentStep.setError(msg);
      report.addError(msg);
    }
    return report;
  }

  private JsonNode generateFrame() {
    Task step = this.addSubTask("Generating JSON-LD frame from schema");
    step.start();
    JsonLdFrameGenerator frameGenerator = new JsonLdFrameGenerator();
    JsonNode frame = frameGenerator.generate(schema.getMetadata());
    step.complete("Frame generated");
    return frame;
  }

  private void frameTransformImport(JsonNode frame, Model rdfModel, HarvestReport report)
      throws IOException {
    Task currentStep = this.addSubTask("Framing JSON-LD");
    currentStep.start();
    JsonLdFramer framer = new JsonLdFramer();
    JsonNode framedJson = framer.frame(rdfModel, frame);
    currentStep.complete("JSON-LD framed successfully");

    currentStep = this.addSubTask("Importing data");
    currentStep.start();
    Set<String> targetColumns = resolveTargetColumns(frame);
    Map<String, String> reverseContext = buildReverseContext(frame.get("@context"), targetColumns);
    List<JsonNode> graphItems = extractGraphItems(framedJson);
    importGraphItems(graphItems, frame, reverseContext, report);
    int warningCount = report.getWarnings().size();
    String importMsg =
        "Imported "
            + report.getResourcesImported()
            + " resources"
            + (warningCount > 0 ? ", " + warningCount + " skipped" : "");
    currentStep.complete(importMsg);

    log.info("Harvest complete: {} resources", report.getResourcesImported());
  }

  Map<String, String> buildReverseContext(JsonNode context) {
    return buildReverseContext(context, null);
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
      if (value.isObject() && value.has("@id") && !isReferenceEntry(value)) {
        if (allowedColumns != null && !allowedColumns.contains(columnName)) {
          continue;
        }
        String predicateIri = value.get("@id").asText();
        reverseMap.putIfAbsent(predicateIri, columnName);
      }
    }
    return reverseMap;
  }

  boolean isReferenceEntry(JsonNode contextEntry) {
    JsonNode typeNode = contextEntry.get("@type");
    return typeNode != null && "@id".equals(typeNode.asText());
  }

  List<JsonNode> extractGraphItems(JsonNode framedJson) {
    List<JsonNode> items = new ArrayList<>();
    if (framedJson.has("@graph") && framedJson.get("@graph").isArray()) {
      for (JsonNode item : framedJson.get("@graph")) {
        items.add(item);
      }
    } else if (framedJson.has("@type")) {
      items.add(framedJson);
    }
    return items;
  }

  private void importGraphItems(
      List<JsonNode> items,
      JsonNode frame,
      Map<String, String> reverseContext,
      HarvestReport report) {
    String tableNameForCatalog = findTableNameForType(frame, "dcat:Catalog");
    String tableNameForDataset = findTableNameForType(frame, "dcat:Dataset");
    String resourcesTableName =
        tableNameForCatalog != null
            ? tableNameForCatalog
            : (tableNameForDataset != null ? tableNameForDataset : "Resources");

    List<Row> resourceRows = new ArrayList<>();
    for (JsonNode item : items) {
      String itemType = extractType(item);
      if (isDcatResource(itemType)) {
        try {
          Row row = nodeToRow(item, itemType, reverseContext);
          resourceRows.add(row);
        } catch (Exception e) {
          String id = item.has("@id") ? item.get("@id").asText() : "unknown";
          report.addWarning("Skipped resource " + id + ": " + e.getMessage());
        }
      }
    }

    if (!resourceRows.isEmpty()) {
      Table resourcesTable = schema.getTable(resourcesTableName);
      if (resourcesTable == null) {
        report.addError("Table '" + resourcesTableName + "' not found in schema");
        return;
      }
      ensureDefaultRefRecordsExist(resourcesTable);
      for (Row row : resourceRows) {
        try {
          resourcesTable.save(List.of(row));
          report.incrementResources(1);
        } catch (Exception e) {
          String rowId = row.getColumnNames().contains("id") ? row.getString("id") : "unknown";
          String warning = "Skipped resource " + rowId + ": " + e.getMessage();
          log.warn(warning, e);
          report.addWarning(warning);
        }
      }
    } else {
      report.addWarning("No DCAT resources found in framed data");
    }
  }

  private static final Pattern JS_OBJ_ID_PATTERN =
      Pattern.compile("[{,]\\s*id\\s*:\\s*['\"]([^'\"]+)['\"]");

  private void ensureDefaultRefRecordsExist(Table table) {
    for (Column col : table.getMetadata().getColumns()) {
      if (!col.isRef()) {
        continue;
      }
      String defaultValue = col.getDefaultValue();
      if (defaultValue == null || !defaultValue.startsWith("=")) {
        continue;
      }
      Table refTable = schema.getTable(col.getRefTableName());
      if (refTable == null) {
        continue;
      }
      List<Column> pkCols = refTable.getMetadata().getPrimaryKeyColumns();
      if (pkCols.size() != 1) {
        continue;
      }
      String pkName = pkCols.get(0).getName();
      Matcher matcher = JS_OBJ_ID_PATTERN.matcher(defaultValue);
      if (!matcher.find()) {
        continue;
      }
      String defaultPkValue = matcher.group(1);
      boolean exists =
          refTable.retrieveRows().stream()
              .anyMatch(r -> defaultPkValue.equals(r.getString(pkName)));
      if (!exists) {
        Row placeholder = new Row();
        placeholder.set(pkName, defaultPkValue);
        placeholder.setDraft(true);
        refTable.save(List.of(placeholder));
        log.info(
            "Created draft placeholder in '{}' with {}='{}'",
            col.getRefTableName(),
            pkName,
            defaultPkValue);
      }
    }
  }

  private Set<String> resolveTargetColumns(JsonNode frame) {
    String tableName = findTableNameForType(frame, "dcat:Catalog");
    if (tableName == null) {
      tableName = findTableNameForType(frame, "dcat:Dataset");
    }
    if (tableName == null) {
      tableName = "Resources";
    }
    if (schema == null) {
      return null;
    }
    Table table = schema.getTable(tableName);
    if (table == null) {
      return null;
    }
    return table.getMetadata().getColumns().stream()
        .filter(col -> col.getKey() != 1)
        .map(Column::getIdentifier)
        .collect(Collectors.toSet());
  }

  private String findTableNameForType(JsonNode frame, String dcatType) {
    JsonNode context = frame.get("@context");
    if (context == null || !context.isObject()) {
      return null;
    }
    for (Iterator<Map.Entry<String, JsonNode>> it = context.fields(); it.hasNext(); ) {
      Map.Entry<String, JsonNode> entry = it.next();
      JsonNode value = entry.getValue();
      if (value.isObject() && dcatType.equals(value.path("@type").asText(null))) {
        return entry.getKey();
      }
    }
    return null;
  }

  private String extractType(JsonNode item) {
    JsonNode typeNode = item.get("@type");
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

  private boolean isDcatResource(String itemType) {
    return "dcat:Catalog".equals(itemType)
        || "dcat:Dataset".equals(itemType)
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
              if (columnName == null) {
                return;
              }
              if (value.isTextual()) {
                row.set(columnName, value.asText());
              } else if (value.isObject()) {
                if (value.has("@value")) {
                  row.set(columnName, value.get("@value").asText());
                } else if (value.has("@id")) {
                  row.set(columnName, value.get("@id").asText());
                }
              } else if (value.isArray()) {
                List<String> elements = new ArrayList<>();
                for (JsonNode element : value) {
                  if (element.isTextual()) {
                    elements.add(element.asText());
                  } else if (element.has("@value")) {
                    elements.add(element.get("@value").asText());
                  } else if (element.has("@id")) {
                    elements.add(element.get("@id").asText());
                  }
                }
                if (!elements.isEmpty()) {
                  row.set(columnName, elements.toArray(new String[0]));
                }
              }
            });

    if (!row.getColumnNames().contains("type")) {
      String inferredType = inferTypeFromDcatType(itemType);
      if (inferredType != null) {
        row.set("type", new String[] {inferredType});
      }
    }

    if (!row.getColumnNames().contains("id") && item.has("@id")) {
      String extractedId = extractIdFromIri(item.get("@id").asText(), itemType);
      if (extractedId != null) {
        row.set("id", extractedId);
      }
    }

    return row;
  }

  String inferTypeFromDcatType(String dcatType) {
    if ("dcat:Catalog".equals(dcatType)) {
      return "Catalogue";
    }
    if ("dcat:Dataset".equals(dcatType)) {
      return "Cohort study";
    }
    return null;
  }

  private String extractIdFromIri(String iri, String itemType) {
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
    String prefix =
        "dcat:Catalog".equals(itemType)
            ? "catalog-"
            : ("dcat:Dataset".equals(itemType) ? "dataset-" : "");
    return prefix + lastSegment;
  }

  static Model parseTurtle(String rdfTurtle) throws IOException {
    Model model = new TreeModel();
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(new StatementCollector(model));
    parser.parse(new StringReader(rdfTurtle));
    return model;
  }
}
