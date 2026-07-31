package org.molgenis.emx2.io.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

class Emx2ModuleRoundtripTest {

  @Test
  void moduleTableTypeAndColumnValuesRoundtrip() {
    SchemaMetadata original = buildSchema();

    List<Row> rows = Emx2.toRowList(original);
    SchemaMetadata restored = Emx2.fromRowList(rows);

    assertModuleTableType(restored);
    assertMultiParentExtends(restored);
    assertModuleArrayValuesPreserved(restored);
    assertEnumValuesPreserved(restored);
  }

  private SchemaMetadata buildSchema() {
    SchemaMetadata schema = new SchemaMetadata();

    TableMetadata base = table("Base");
    base.add(column("id").setType(ColumnType.STRING).setKey(1));
    base.add(
        column("activeModules").setType(ColumnType.MODULE_ARRAY).setValues("ModuleA", "ModuleB"));
    base.add(column("category").setType(ColumnType.ENUM).setValues("alpha", "beta", "gamma"));
    schema.create(base);

    TableMetadata leftParent = table("LeftParent");
    leftParent.setInheritNames("Base");
    leftParent.add(column("leftCol").setType(ColumnType.STRING));
    schema.create(leftParent);

    TableMetadata rightParent = table("RightParent");
    rightParent.setInheritNames("Base");
    rightParent.add(column("rightCol").setType(ColumnType.STRING));
    schema.create(rightParent);

    TableMetadata moduleA = table("ModuleA");
    moduleA.setTableType(TableType.MODULE);
    moduleA.setInheritNames("Base");
    moduleA.add(column("moduleAcol").setType(ColumnType.STRING));
    schema.create(moduleA);

    TableMetadata diamond = table("Diamond");
    diamond.setInheritNames("LeftParent", "RightParent");
    diamond.add(column("ownCol").setType(ColumnType.STRING));
    schema.create(diamond);

    return schema;
  }

  private void assertModuleTableType(SchemaMetadata schema) {
    TableMetadata moduleA = schema.getTableMetadata("ModuleA");
    assertNotNull(moduleA, "ModuleA table must survive roundtrip");
    assertEquals(TableType.MODULE, moduleA.getTableType(), "ModuleA tableType must be MODULE");
  }

  private void assertMultiParentExtends(SchemaMetadata schema) {
    TableMetadata diamond = schema.getTableMetadata("Diamond");
    assertNotNull(diamond, "Diamond table must survive roundtrip");
    List<String> parents = diamond.getInheritNames();
    assertEquals(2, parents.size(), "Diamond must have 2 parents");
    assertEquals("LeftParent", parents.get(0));
    assertEquals("RightParent", parents.get(1));
  }

  private void assertModuleArrayValuesPreserved(SchemaMetadata schema) {
    TableMetadata base = schema.getTableMetadata("Base");
    assertNotNull(base, "Base table must survive roundtrip");
    Column activeModules = base.getLocalColumn("activeModules");
    assertNotNull(activeModules, "activeModules column must survive roundtrip");
    assertEquals(ColumnType.MODULE_ARRAY, activeModules.getColumnType());
    List<String> values = activeModules.getValues();
    assertNotNull(values, "MODULE_ARRAY column values must not be null after roundtrip");
    assertEquals(List.of("ModuleA", "ModuleB"), values);
  }

  private void assertEnumValuesPreserved(SchemaMetadata schema) {
    TableMetadata base = schema.getTableMetadata("Base");
    assertNotNull(base, "Base table must survive roundtrip");
    Column category = base.getLocalColumn("category");
    assertNotNull(category, "category column must survive roundtrip");
    assertEquals(ColumnType.ENUM, category.getColumnType());
    List<String> values = category.getValues();
    assertNotNull(values, "ENUM column values must not be null after roundtrip");
    assertEquals(List.of("alpha", "beta", "gamma"), values);
  }
}
