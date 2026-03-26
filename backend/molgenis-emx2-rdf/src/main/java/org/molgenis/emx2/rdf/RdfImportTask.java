package org.molgenis.emx2.rdf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.rdf.ReverseAnnotationMapper.ColumnMapping;
import org.molgenis.emx2.rdf.RowBuilder.RowBuildResult;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.utils.TableSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RdfImportTask extends Task {
  private static final Logger log = LoggerFactory.getLogger(RdfImportTask.class);
  private final Schema schema;
  private final String sourceUrl;
  private final InputStream sourceStream;
  private final String formatHint;

  public RdfImportTask(Schema schema, String url) {
    super("RDF import from " + url);
    this.schema = schema;
    this.sourceUrl = url;
    this.sourceStream = null;
    this.formatHint = null;
  }

  public RdfImportTask(Schema schema, InputStream input, String formatHint) {
    super("RDF import from file");
    this.schema = schema;
    this.sourceUrl = null;
    this.sourceStream = input;
    this.formatHint = formatHint;
  }

  @Override
  public void run() {
    start();
    try {
      Task mapTask = addSubTask("Building predicate map from schema annotations");
      mapTask.start();
      Map<IRI, List<ColumnMapping>> predicateMap =
          ReverseAnnotationMapper.buildPredicateMap(schema);
      String mapMsg = "Built predicate map with " + predicateMap.size() + " predicates";
      mapTask.complete(mapMsg);
      log.info(mapMsg);

      Task parseTask = addSubTask("Parsing RDF data");
      parseTask.start();
      FilteringRdfHandler handler = new FilteringRdfHandler(predicateMap);
      if (sourceUrl != null) {
        try (RdfFetcher.FetchResult fetch = RdfFetcher.fetchUrlWithFormat(sourceUrl)) {
          RdfFetcher.parse(fetch.inputStream(), fetch.contentType(), handler);
        }
      } else {
        RdfFetcher.parse(sourceStream, formatHint, handler);
      }
      String parseMsg =
          "Parsed "
              + handler.getMatchedData().size()
              + " subjects, "
              + handler.getUnmatchedCount()
              + " unmatched triples";
      parseTask.complete(parseMsg);
      log.info(parseMsg);

      Task buildTask = addSubTask("Building rows from RDF data");
      buildTask.start();
      RowBuildResult result =
          RowBuilder.buildRows(handler.getMatchedData(), handler.getTypeMap(), schema);
      String buildMsg =
          "Built rows for "
              + result.rowsByTable().size()
              + " tables "
              + result.rowsByTable().entrySet().stream()
                  .map(e -> e.getKey() + "=" + e.getValue().size())
                  .toList()
              + ", "
              + result.warnings().size()
              + " warnings";
      buildTask.complete(buildMsg);
      log.info(buildMsg);
      result.warnings().stream().limit(5).forEach(w -> log.info("  Warning: {}", w));

      Task saveTask = addSubTask("Saving rows to database");
      saveTask.start();
      List<TableMetadata> sortedTableMeta =
          result.rowsByTable().keySet().stream()
              .map(name -> schema.getTable(name))
              .filter(t -> t != null)
              .map(Table::getMetadata)
              .collect(Collectors.toCollection(ArrayList::new));
      TableSort.sortTableByDependency(sortedTableMeta);

      Map<String, Integer> savedCounts = new java.util.LinkedHashMap<>();
      List<String> saveWarnings = new ArrayList<>();
      List<String> pendingTables =
          sortedTableMeta.stream()
              .map(TableMetadata::getTableName)
              .collect(Collectors.toCollection(ArrayList::new));

      boolean progress = true;
      while (!pendingTables.isEmpty() && progress) {
        progress = false;
        List<String> stillPending = new ArrayList<>();
        for (String tableName : pendingTables) {
          List<Row> rows = result.rowsByTable().get(tableName);
          Table table = schema.getTable(tableName);
          if (table != null && rows != null) {
            try {
              table.save(rows);
              savedCounts.put(tableName, rows.size());
              progress = true;
            } catch (Exception e) {
              log.info("Save failed for table '{}', will retry: {}", tableName, e.getMessage());
              stillPending.add(tableName);
            }
          }
        }
        pendingTables = stillPending;
      }

      Map<String, List<Row>> tablesNeedingUpdate = new HashMap<>();
      List<String> permanentlyFailed = new ArrayList<>();
      for (String tableName : pendingTables) {
        List<Row> rows = result.rowsByTable().get(tableName);
        Table table = schema.getTable(tableName);
        if (table == null || rows == null) {
          continue;
        }
        List<Row> strippedRows = stripOptionalRefColumns(rows, table.getMetadata());
        try {
          table.save(strippedRows);
          savedCounts.put(tableName, strippedRows.size());
          tablesNeedingUpdate.put(tableName, rows);
        } catch (Exception e) {
          permanentlyFailed.add(tableName);
          savedCounts.put(tableName, 0);
          String warning =
              "Skipped table '" + tableName + "' (" + rows.size() + " rows): " + e.getMessage();
          saveWarnings.add(warning);
          log.debug(warning);
        }
      }

      for (Map.Entry<String, List<Row>> entry : tablesNeedingUpdate.entrySet()) {
        String tableName = entry.getKey();
        Table table = schema.getTable(tableName);
        if (table != null) {
          try {
            table.save(entry.getValue());
          } catch (Exception e) {
            saveWarnings.add("Partial save for '" + tableName + "': " + e.getMessage());
          }
        }
      }

      int totalSaved = savedCounts.values().stream().mapToInt(Integer::intValue).sum();
      String saveMsg = "Saved " + totalSaved + " rows";
      if (!saveWarnings.isEmpty()) {
        saveMsg +=
            "; warnings: "
                + saveWarnings.get(0)
                + (saveWarnings.size() > 1 ? " (+" + (saveWarnings.size() - 1) + " more)" : "");
      }
      saveTask.complete(saveMsg);

      StringBuilder summary = new StringBuilder("Import completed: ");
      for (Map.Entry<String, Integer> entry : savedCounts.entrySet()) {
        summary.append(entry.getKey()).append("=").append(entry.getValue()).append(" ");
      }
      List<String> allWarnings = new ArrayList<>(result.warnings());
      allWarnings.addAll(saveWarnings);
      if (!allWarnings.isEmpty()) {
        summary.append("(").append(allWarnings.size()).append(" warnings)");
      }
      complete(summary.toString().trim());

    } catch (Exception e) {
      completeWithError("RDF import failed: " + e.getMessage());
    }
  }

  private static List<Row> stripOptionalRefColumns(List<Row> rows, TableMetadata tableMeta) {
    List<String> optionalRefColumns =
        tableMeta.getColumns().stream()
            .filter(
                col ->
                    col.isReference()
                        && col.getKey() == 0
                        && !col.isRequired()
                        && col.getColumnType() != ColumnType.REFBACK)
            .map(Column::getName)
            .toList();

    if (optionalRefColumns.isEmpty()) {
      return rows;
    }

    return rows.stream()
        .map(
            row -> {
              Row stripped = new Row(row);
              optionalRefColumns.forEach(col -> stripped.set(col, null));
              return stripped;
            })
        .toList();
  }
}
