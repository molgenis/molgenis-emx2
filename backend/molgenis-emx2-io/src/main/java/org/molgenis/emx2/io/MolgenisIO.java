package org.molgenis.emx2.io;

import static org.molgenis.emx2.io.ImportSchemaTask.MOLGENIS_ONTOLOGIES;
import static org.molgenis.emx2.io.emx2.Emx2.outputMetadata;
import static org.molgenis.emx2.io.emx2.Emx2Members.outputRoles;
import static org.molgenis.emx2.io.emx2.Emx2Settings.outputSettings;
import static org.molgenis.emx2.io.emx2.Emx2Tables.outputTable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.io.emx1.Emx1;
import org.molgenis.emx2.io.tablestore.*;

/** Short hands for running the tasks */
public class MolgenisIO {

  private MolgenisIO() {
    // hide constructor
  }

  private static void outputAll(TableStore store, Schema schema) {
    outputMetadata(store, schema);
    outputRoles(store, schema);
    outputSettings(store, schema);
    outputOntologies(store, schema);

    // user data goes on one sheet per table
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      if (TableType.TABLES.equals(table.getMetadata().getTableType())) {
        outputTable(store, table);
      }
    }
  }

  private static void outputOntologies(TableStore store, Schema schema) {
    List<Row> ontologyRows = new ArrayList<>(); // might get memory hungry on very large ontologies
    for (String tableName : schema.getTableNames()) {
      Table table = schema.getTable(tableName);
      if (TableType.ONTOLOGIES.equals(table.getMetadata().getTableType())) {
        ontologyRows.addAll(
            table.retrieveRows().stream()
                .map(
                    r -> {
                      // ensure 'ontology' is first element
                      Row row = new Row();
                      row.setString("ontology", table.getName());
                      // skip "mg_"
                      for (Map.Entry<String, Object> entry : r.getValueMap().entrySet()) {
                        if (!entry.getKey().startsWith("mg_")) {
                          row.getValueMap().put(entry.getKey(), entry.getValue());
                        }
                      }
                      return row;
                    })
                .toList());
      }
    }
    if (ontologyRows.size() > 0) {
      store.writeTable(
          MOLGENIS_ONTOLOGIES,
          List.of("ontology", "order", "name", "parent", "code", "definition", "ontologyURI"),
          ontologyRows);
    }
  }

  public static void toDirectory(Path directory, Schema schema) {
    outputAll(new TableStoreForCsvFilesDirectory(directory), schema);
  }

  public static void toZipFile(Path zipFile, Schema schema) {
    outputAll(new TableStoreForCsvInZipFile(zipFile), schema);
  }

  public static void toExcelFile(Path excelFile, Schema schema) {
    outputAll(new TableStoreForXlsxFile(excelFile), schema);
  }

  public static void toEmx1ExcelFile(Path excelFile, Schema schema) {
    executeEmx1Export(new TableStoreForXlsxFile(excelFile), schema);
  }

  private static void executeEmx1Export(TableStore store, Schema schema) {
    // write metadata
    store.writeTable(
        "entities", List.of("UNSUPPORTED"), Emx1.getEmx1Entities(schema.getMetadata()));
    store.writeTable(
        "attributes", List.of("UNSUPPORTED"), Emx1.getEmx1Attributes(schema.getMetadata()));
    // write data
    for (String tableName : schema.getTableNames()) {
      outputTable(store, schema.getTable(tableName));
    }
  }

  public static void toZipFile(Path zipFile, Table table) {
    outputTable(new TableStoreForCsvInZipFile(zipFile), table);
  }

  public static void toExcelFile(Path excelFile, Table table) {
    outputTable(new TableStoreForXlsxFile(excelFile), table);
  }

  public static void toCsvFile(Path csvFile, Table table) {
    outputTable(new TableStoreForCsvFile(csvFile), table);
  }

  public static void fromDirectory(Path directory, Schema schema, boolean strict) {
    new ImportDirectoryTask(directory, schema, strict).run();
  }

  public static void fromZipFile(Path zipFile, Schema schema, boolean strict) {
    new ImportCsvZipTask(zipFile, schema, strict).run();
  }

  public static void importFromExcelFile(Path excelFile, Schema schema, boolean strict) {
    new ImportExcelTask(excelFile, schema, strict).run();
  }

  public static void fromStore(TableStore store, Schema schema, boolean strict) {
    new ImportSchemaTask(store, schema, strict).run();
  }
}
