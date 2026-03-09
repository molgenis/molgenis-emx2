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
  private static final String DCAT_CATALOG = "dcat:Catalog";
  private static final String DCAT_DATASET = "dcat:Dataset";
  private static final String JSON_LD_VALUE = "@value";
  private static final String JSON_LD_TYPE = "@type";
  private static final String JSON_LD_GRAPH = "@graph";
  private static final String JSON_LD_ID = "@id";
  private static final String HARVEST_FAILED = "Harvest failed: ";

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
    this.start();
    HarvestReport report = new HarvestReport();
    try {
      Model rdfModel = stepFetch();
      FrameResult frameResult = stepFrame(rdfModel);
      List<Row> rows = stepMap(frameResult, report);
      stepImport(frameResult.frame, rows, report);

      if (report.hasErrors()) {
        this.setError("Completed with " + report.getErrors().size() + " error(s)");
      } else {
        this.complete(report.getResourcesImported() + " resources imported");
      }
    } catch (Exception e) {
      this.setError(e.getMessage());
    }
  }

  public HarvestReport harvestRdf(String rdfTurtle) {
    this.start();
    HarvestReport report = new HarvestReport();
    try {
      Task fetchStep = this.addSubTask("Fetch");
      fetchStep.start();
      Model rdfModel = parseTurtle(rdfTurtle);
      fetchStep.complete(rdfModel.size() + " triples parsed");

      FrameResult frameResult = stepFrame(rdfModel);
      List<Row> rows = stepMap(frameResult, report);
      stepImport(frameResult.frame, rows, report);
    } catch (Exception e) {
      this.setError(e.getMessage());
      report.addError(e.getMessage());
    }
    return report;
  }

  private Model stepFetch() throws IOException {
    Task step = this.addSubTask("Fetch");
    step.start();
    try {
      RdfFetcher fetcher = new RdfFetcher();
      Model rdfModel = fetcher.fetchRecursively(catalogUrl, MAX_DEPTH, MAX_CALLS);
      step.complete(rdfModel.size() + " triples fetched");
      return rdfModel;
    } catch (Exception e) {
      step.setError(e.getMessage());
      throw e;
    }
  }

  private FrameResult stepFrame(Model rdfModel) throws IOException {
    Task step = this.addSubTask("Frame");
    step.start();
    try {
      JsonLdFrameGenerator frameGenerator = new JsonLdFrameGenerator();
      JsonNode frame = frameGenerator.generate(schema.getMetadata());
      JsonLdFramer framer = new JsonLdFramer();
      JsonNode framedJson = framer.frame(rdfModel, frame);
      List<JsonNode> graphItems = extractGraphItems(framedJson);
      step.complete(graphItems.size() + " resources framed");
      return new FrameResult(frame, framedJson);
    } catch (Exception e) {
      step.setError(e.getMessage());
      throw e;
    }
  }

  private List<Row> stepMap(FrameResult frameResult, HarvestReport report) {
    Task step = this.addSubTask("Map");
    step.start();
    try {
      Set<String> targetColumns = resolveTargetColumns(frameResult.frame);
      Map<String, String> reverseContext =
          buildReverseContext(frameResult.frame.get("@context"), targetColumns);
      List<JsonNode> graphItems = extractGraphItems(frameResult.framedJson);
      List<Row> rows = collectResourceRows(graphItems, reverseContext, report);
      step.complete(rows.size() + " rows: " + countByType(rows));
      return rows;
    } catch (Exception e) {
      step.setError(e.getMessage());
      throw e;
    }
  }

  private void stepImport(JsonNode frame, List<Row> rows, HarvestReport report) {
    Task step = this.addSubTask("Import");
    step.start();
    try {
      String tableNameForCatalog = findTableNameForType(frame, DCAT_CATALOG);
      String tableNameForDataset = findTableNameForType(frame, DCAT_DATASET);
      String tableName = resolveResourcesTableName(tableNameForCatalog, tableNameForDataset);
      saveResourceRows(step, rows, tableName, report);
      int warnings = report.getWarnings().size();
      String msg = report.getResourcesImported() + " saved (" + countByType(rows) + ")";
      if (warnings > 0) {
        msg += ", " + warnings + " failed";
      }
      step.complete(msg);
    } catch (Exception e) {
      step.setError(e.getMessage());
      throw e;
    }
  }

  private record FrameResult(JsonNode frame, JsonNode framedJson) {}

  private String countByType(List<Row> rows) {
    Map<String, Integer> counts = new HashMap<>();
    for (Row row : rows) {
      String[] types = row.getStringArray("type");
      if (types != null) {
        for (String type : types) {
          counts.merge(type, 1, Integer::sum);
        }
      }
    }
    return counts.toString();
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

  List<JsonNode> extractGraphItems(JsonNode framedJson) {
    List<JsonNode> items = new ArrayList<>();
    if (framedJson.has(JSON_LD_GRAPH) && framedJson.get(JSON_LD_GRAPH).isArray()) {
      for (JsonNode item : framedJson.get(JSON_LD_GRAPH)) {
        items.add(item);
      }
    } else if (framedJson.has(JSON_LD_TYPE)) {
      items.add(framedJson);
    }
    return items;
  }

  private String resolveResourcesTableName(String tableNameForCatalog, String tableNameForDataset) {
    if (tableNameForCatalog != null) {
      return tableNameForCatalog;
    }
    if (tableNameForDataset != null) {
      return tableNameForDataset;
    }
    return "Resources";
  }

  private List<Row> collectResourceRows(
      List<JsonNode> items, Map<String, String> reverseContext, HarvestReport report) {
    List<Row> resourceRows = new ArrayList<>();
    for (JsonNode item : items) {
      String itemType = extractType(item);
      if (isDcatResource(itemType)) {
        try {
          Row row = nodeToRow(item, itemType, reverseContext);
          resourceRows.add(row);
        } catch (Exception e) {
          String id = item.has(JSON_LD_ID) ? item.get(JSON_LD_ID).asText() : "unknown";
          report.addWarning("Skipped resource " + id + ": " + e.getMessage());
        }
      }
    }
    return resourceRows;
  }

  private void saveResourceRows(
      Task step, List<Row> resourceRows, String resourcesTableName, HarvestReport report) {
    if (!resourceRows.isEmpty()) {
      Table resourcesTable = schema.getTable(resourcesTableName);
      if (resourcesTable == null) {
        report.addError("Table '" + resourcesTableName + "' not found in schema");
        return;
      }
      ensureDefaultRefRecordsExist(resourcesTable);
      int failed = 0;
      for (Row row : resourceRows) {
        try {
          resourcesTable.save(List.of(row));
          report.incrementResources(1);
        } catch (Exception e) {
          failed++;
          String rowId = row.getColumnNames().contains("id") ? row.getString("id") : "unknown";
          String warning = "Failed resource " + rowId + ": " + e.getMessage();
          log.warn(warning, e);
          step.addSubTask(warning).setError();
          report.addWarning(warning);
        }
      }
      String msg =
          report.getResourcesImported()
              + " saved to "
              + resourcesTableName
              + " ("
              + countByType(resourceRows)
              + ")";
      if (failed > 0) {
        msg += ", " + failed + " failed";
      }
      step.addSubTask(msg).complete();
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

  static Model parseTurtle(String rdfTurtle) throws IOException {
    Model model = new TreeModel();
    RDFParser parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(new StatementCollector(model));
    parser.parse(new StringReader(rdfTurtle));
    return model;
  }
}
