package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;

class Emx2ModuleScalarRoundtripTest {

  @Test
  void moduleScalarColumnTypeRoundtrips() {
    SchemaMetadata original = buildSchema();

    List<Row> rows = Emx2.toRowList(original);
    SchemaMetadata restored = Emx2.fromRowList(rows);

    assertModuleTableType(restored);
    assertModuleScalarColumnPresent(restored);
    assertExplicitValuesPreserved(restored);
  }

  @Test
  void csvExportDoesNotFreezeScalarModuleDerivedValues() {
    SchemaMetadata schema = new SchemaMetadata();

    TableMetadata host = table("Host");
    host.add(column("id").setType(ColumnType.STRING).setKey(1));
    schema.create(host);

    TableMetadata modA = table("ModA");
    modA.setTableType(TableType.MODULE);
    modA.setInheritNames("Host");
    modA.add(column("colA").setType(ColumnType.STRING));
    schema.create(modA);

    TableMetadata modB = table("ModB");
    modB.setTableType(TableType.MODULE);
    modB.setInheritNames("Host");
    modB.add(column("colB").setType(ColumnType.STRING));
    schema.create(modB);

    host.add(column("modType").setType(ColumnType.MODULE));

    Column modType = host.getLocalColumn("modType");
    assertNull(modType.getValues(), "getValues() must be null for derived-default MODULE column");

    List<Row> rows = Emx2.toRowList(schema);

    Row modTypeRow = null;
    for (Row row : rows) {
      if ("modType".equals(row.getString("columnName"))
          && "Host".equals(row.getString("tableName"))) {
        modTypeRow = row;
        break;
      }
    }
    assertNotNull(modTypeRow, "modType column must appear in CSV export rows");

    String exportedValues = modTypeRow.getString("values");
    assertTrue(
        exportedValues == null || exportedValues.isEmpty(),
        "CSV export must NOT freeze derived values — 'values' column must be empty for a derived-default MODULE column, got: "
            + exportedValues);

    SchemaMetadata restored = Emx2.fromRowList(rows);
    Column restoredModType = restored.getTableMetadata("Host").getLocalColumn("modType");
    assertNotNull(restoredModType, "modType column must survive roundtrip");
    assertEquals(ColumnType.MODULE, restoredModType.getColumnType(), "type must remain MODULE");
    assertNull(
        restoredModType.getValues(),
        "getValues() must remain null after roundtrip (derived-default not frozen)");
  }

  private SchemaMetadata buildSchema() {
    SchemaMetadata schema = new SchemaMetadata();

    TableMetadata host = table("Host");
    host.add(column("id").setType(ColumnType.STRING).setKey(1));
    host.add(column("modType").setType(ColumnType.MODULE).setValues("ModuleA"));
    schema.create(host);

    TableMetadata moduleA = table("ModuleA");
    moduleA.setTableType(TableType.MODULE);
    moduleA.setInheritNames("Host");
    moduleA.add(column("moduleAcol").setType(ColumnType.STRING));
    schema.create(moduleA);

    return schema;
  }

  private void assertModuleTableType(SchemaMetadata schema) {
    TableMetadata moduleA = schema.getTableMetadata("ModuleA");
    assertNotNull(moduleA, "ModuleA table must survive roundtrip");
    assertEquals(TableType.MODULE, moduleA.getTableType(), "ModuleA tableType must be MODULE");
  }

  private void assertModuleScalarColumnPresent(SchemaMetadata schema) {
    TableMetadata host = schema.getTableMetadata("Host");
    assertNotNull(host, "Host table must survive roundtrip");
    Column modType = host.getLocalColumn("modType");
    assertNotNull(modType, "modType column must survive roundtrip");
    assertEquals(ColumnType.MODULE, modType.getColumnType(), "modType must be MODULE type");
  }

  private void assertExplicitValuesPreserved(SchemaMetadata schema) {
    TableMetadata host = schema.getTableMetadata("Host");
    Column modType = host.getLocalColumn("modType");
    List<String> values = modType.getValues();
    assertNotNull(values, "Explicit MODULE column values must survive roundtrip");
    assertEquals(List.of("ModuleA"), values, "Explicit values must be preserved exactly");
  }
}
