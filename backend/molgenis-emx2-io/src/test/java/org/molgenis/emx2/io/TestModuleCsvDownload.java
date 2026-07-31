package org.molgenis.emx2.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TableType.MODULE;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.emx2.Emx2Tables;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInMemory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TestModuleCsvDownload {

  private static final String SCHEMA_PREFIX = "TestModuleCsvDownload";

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  private Schema freshSchema(String suffix) {
    String name = SCHEMA_PREFIX + suffix;
    db.dropSchemaIfExists(name);
    return db.createSchema(name);
  }

  @Test
  void csvExportIncludesModuleColumnAndValue() {
    Schema schema = freshSchema("Export");

    schema.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    schema.create(table("Mod").setTableType(MODULE).setInheritNames("Root").add(column("modCol")));
    schema
        .getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    schema
        .getTable("Root")
        .insert(row("id", "r1", "rootCol", "rootValue", "panels", "Mod", "modCol", "modValue"));

    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    Emx2Tables.outputTable(store, schema.getTable("Root"));

    assertTrue(store.containsTable("Root"), "Root table must be exported");
    String csv = store.getCsvString("Root");
    assertNotNull(csv, "Exported CSV must not be null");
    assertTrue(
        csv.contains("modCol"),
        "Exported CSV header must contain the module column 'modCol', got:\n" + csv);
    assertTrue(
        csv.contains("modValue"),
        "Exported CSV must contain the module column value 'modValue', got:\n" + csv);
  }

  @Test
  void csvExportModuleColumnNullForInactiveRow() {
    Schema schema = freshSchema("ExportNull");

    schema.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    schema.create(table("Mod").setTableType(MODULE).setInheritNames("Root").add(column("modCol")));
    schema
        .getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    schema
        .getTable("Root")
        .insert(row("id", "r1", "rootCol", "rootValue", "panels", "Mod", "modCol", "modValue"));
    schema.getTable("Root").insert(row("id", "r2", "rootCol", "noModule"));

    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    Emx2Tables.outputTable(store, schema.getTable("Root"));

    String csv = store.getCsvString("Root");
    assertNotNull(csv);
    assertTrue(
        csv.contains("modCol"),
        "Exported CSV header must contain module column even when some rows have no module, got:\n"
            + csv);
    assertTrue(csv.contains("modValue"), "Active row must export modValue in CSV, got:\n" + csv);
  }

  @Test
  void csvExportReimportRoundTrip() {
    Schema exportSchema = freshSchema("RoundTripExport");

    exportSchema.create(
        table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    exportSchema.create(
        table("Mod").setTableType(MODULE).setInheritNames("Root").add(column("modCol")));
    exportSchema
        .getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    exportSchema
        .getTable("Root")
        .insert(row("id", "r1", "rootCol", "rootValue", "panels", "Mod", "modCol", "modValue"));

    TableStoreForCsvInMemory store = new TableStoreForCsvInMemory();
    Emx2Tables.outputTable(store, exportSchema.getTable("Root"));

    String exportedCsv = store.getCsvString("Root");
    assertTrue(
        exportedCsv.contains("modCol"), "Pre-condition: exported CSV must contain modCol header");
    assertTrue(
        exportedCsv.contains("modValue"), "Pre-condition: exported CSV must contain modValue");

    Schema importSchema = freshSchema("RoundTripImport");
    importSchema.create(
        table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    importSchema.create(
        table("Mod").setTableType(MODULE).setInheritNames("Root").add(column("modCol")));
    importSchema
        .getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    TableStoreForCsvInMemory importStore = new TableStoreForCsvInMemory();
    importStore.setCsvString("Root", exportedCsv);

    Iterable<Row> importedRows = importStore.readTable("Root");
    importSchema.getTable("Root").save(importedRows);

    List<Row> modRows = importSchema.getTable("Mod").retrieveRows();
    assertEquals(1, modRows.size(), "Re-import round-trip: Mod must have exactly one row");
    assertEquals(
        "r1", modRows.get(0).getString("id"), "Re-import round-trip: Mod row must share root PK");
    assertEquals(
        "modValue",
        modRows.get(0).getString("modCol"),
        "Re-import round-trip: modCol value must be preserved");

    List<Row> rootRows = importSchema.getTable("Root").retrieveRows();
    assertEquals(1, rootRows.size(), "Re-import round-trip: Root must have exactly one row");
    assertEquals("r1", rootRows.get(0).getString("id"));
  }
}
