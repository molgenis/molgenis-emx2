package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.ColumnSelection.INHERITED;
import static org.molgenis.emx2.TableMetadata.ColumnSelection.LOCAL;
import static org.molgenis.emx2.TableMetadata.ColumnSelection.MODULES;
import static org.molgenis.emx2.TableMetadata.ColumnSelection.SUBCLASSES;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class TestTableMetadataInheritance {

  private static SchemaMetadata diamondWithModuleFixture() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table("Root", column("id").setPkey(), column("rootCol")),
        table("ParentA", column("paCol")).setInheritNames("Root"),
        table("ParentB", column("pbCol")).setInheritNames("Root"),
        table("Child", column("childCol")).setInheritNames(List.of("ParentA", "ParentB")),
        table("ModuleX", column("modXCol")).setTableType(TableType.MODULE).setInheritNames("Root"));
    return schema;
  }

  private static Set<String> columnNames(List<Column> columns) {
    return columns.stream().map(Column::getName).collect(Collectors.toSet());
  }

  @Test
  void getColumnsLocalReturnsOnlyOwnColumnsPlusInheritedKey() {
    TableMetadata child = diamondWithModuleFixture().getTableMetadata("Child");
    assertEquals(Set.of("id", "childCol"), columnNames(child.getColumns(LOCAL)));
  }

  @Test
  void getColumnsInheritedAddsAllAncestorColumns() {
    TableMetadata child = diamondWithModuleFixture().getTableMetadata("Child");
    assertEquals(
        Set.of("id", "rootCol", "paCol", "pbCol", "childCol"),
        columnNames(child.getColumns(INHERITED)));
  }

  @Test
  void getColumnsSubclassesAddsSubclassColumnsButNotModuleColumns() {
    TableMetadata root = diamondWithModuleFixture().getTableMetadata("Root");
    assertEquals(
        Set.of("id", "rootCol", "paCol", "pbCol", "childCol"),
        columnNames(root.getColumns(INHERITED, SUBCLASSES)));
  }

  @Test
  void getColumnsModulesAddsModuleColumnsButNotSubclassColumns() {
    TableMetadata root = diamondWithModuleFixture().getTableMetadata("Root");
    assertEquals(
        Set.of("id", "rootCol", "modXCol"), columnNames(root.getColumns(INHERITED, MODULES)));
  }

  @Test
  void getColumnsSubclassesAndModulesAddsBoth() {
    TableMetadata root = diamondWithModuleFixture().getTableMetadata("Root");
    assertEquals(
        Set.of("id", "rootCol", "paCol", "pbCol", "childCol", "modXCol"),
        columnNames(root.getColumns(INHERITED, SUBCLASSES, MODULES)));
  }

  @Test
  void getInheritNameReturnsPrimaryParent() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(table("Root", column("id").setPkey()), table("Child").setInheritNames("Root"));

    TableMetadata child = schema.getTableMetadata("Child");
    assertEquals(List.of("Root"), child.getInheritNames());
  }

  @Test
  void getInheritNamesReturnsUnmodifiableView() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(table("Root", column("id").setPkey()), table("Child").setInheritNames("Root"));

    List<String> names = schema.getTableMetadata("Child").getInheritNames();
    assertThrows(UnsupportedOperationException.class, () -> names.add("Extra"));
  }

  @Test
  void setInheritNameReplacesExistingParent() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(
                table("Root", column("id").setPkey()),
                table("OtherRoot", column("id").setPkey()),
                table("Child").setInheritNames("Root"));

    TableMetadata child = schema.getTableMetadata("Child");
    child.setInheritNames("OtherRoot");
    assertEquals(List.of("OtherRoot"), child.getInheritNames());
  }

  @Test
  void setInheritNamesIgnoresDuplicates() {
    TableMetadata table = table("Child");
    table.setInheritNames("Root", "Root");
    assertEquals(List.of("Root"), table.getInheritNames());
  }

  @Test
  void setInheritNamesReplacesAllParents() {
    TableMetadata tableMetadata = table("Child");
    tableMetadata.setInheritNames("OldParent");
    tableMetadata.setInheritNames(List.of("ParentA", "ParentB"));
    assertEquals(List.of("ParentA", "ParentB"), tableMetadata.getInheritNames());
  }

  @Test
  void removeInheritClearsAllParents() {
    TableMetadata tableMetadata = table("Child");
    tableMetadata.setInheritNames("Root");
    tableMetadata.removeInherit();
    assertTrue(tableMetadata.getInheritNames().isEmpty());
  }

  @Test
  void getRootTableWalksSingleChain() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(
                table("Root", column("id").setPkey()),
                table("Middle").setInheritNames("Root"),
                table("Leaf").setInheritNames("Middle"));

    TableMetadata root = schema.getTableMetadata("Leaf").getRootTable();
    assertEquals("Root", root.getTableName());
  }

  @Test
  void getRootTableReturnsItselfWhenNoParent() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(table("Standalone", column("id").setPkey()));
    TableMetadata root = schema.getTableMetadata("Standalone").getRootTable();
    assertEquals("Standalone", root.getTableName());
  }

  @Test
  void getAllInheritNamesIncludesSelfAndAllAncestors() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(
                table("Root", column("id").setPkey()),
                table("Middle").setInheritNames("Root"),
                table("Leaf").setInheritNames("Middle"));

    List<String> names = schema.getTableMetadata("Leaf").getAllInheritNames();
    assertEquals(List.of("Leaf", "Middle", "Root"), names);
  }

  @Test
  void getColumnsIncludesParentColumnsAndOwnColumns() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(
                table("Root", column("id").setPkey(), column("name")),
                table("Child", column("salary")).setInheritNames("Root"));

    List<Column> cols = schema.getTableMetadata("Child").getColumnsWithoutMetadata();
    assertEquals(3, cols.size());
    List<String> colNames = cols.stream().map(Column::getName).toList();
    assertTrue(colNames.contains("id"));
    assertTrue(colNames.contains("name"));
    assertTrue(colNames.contains("salary"));
  }

  @Test
  void getColumnSearchesAllParents() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(
                table("Root", column("id").setPkey(), column("rootCol")),
                table("Middle", column("middleCol")).setInheritNames("Root"),
                table("Leaf", column("leafCol")).setInheritNames("Middle"));

    TableMetadata leaf = schema.getTableMetadata("Leaf");
    assertNotNull(leaf.getColumn("rootCol"));
    assertNotNull(leaf.getColumn("middleCol"));
    assertNotNull(leaf.getColumn("leafCol"));
    assertNull(leaf.getColumn("nonExistent"));
  }

  @Test
  void getSubclassTablesUsesInheritNames() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(table("Root", column("id").setPkey()), table("Child").setInheritNames("Root"));

    List<TableMetadata> subclasses = schema.getTableMetadata("Root").getSubclassTables();
    assertEquals(1, subclasses.size());
    assertEquals("Child", subclasses.get(0).getTableName());
  }

  @Test
  void getNonInheritedColumnsExcludesParentColumns() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(
                table("Root", column("id").setPkey(), column("name")),
                table("Child", column("salary")).setInheritNames("Root"));

    List<Column> nonInherited = schema.getTableMetadata("Child").getNonInheritedColumns();
    assertEquals(1, nonInherited.size());
    assertEquals("salary", nonInherited.get(0).getName());
  }

  @Test
  void addColumnThrowsWhenAlreadyInParent() {
    SchemaMetadata schema =
        new SchemaMetadata()
            .create(
                table("Root", column("id").setPkey(), column("name")),
                table("Child").setInheritNames("Root"));

    assertThrows(
        MolgenisException.class, () -> schema.getTableMetadata("Child").add(column("name")));
  }

  @Test
  void tableTypeModuleHasIsModuleHelper() {
    assertTrue(TableType.MODULE.isModule());
    assertFalse(TableType.DATA.isModule());
    assertFalse(TableType.ONTOLOGIES.isModule());
  }

  @Test
  void columnTypeModuleArrayIsEnumFamily() {
    assertEquals(ColumnType.ENUM_ARRAY, ColumnType.MODULE_ARRAY.getBaseType());
    assertTrue(ColumnType.MODULE_ARRAY.isEnum());
  }

  @Test
  void columnTypeModuleArrayIsNotReferenceType() {
    assertFalse(ColumnType.MODULE_ARRAY.isReference());
    assertFalse(ColumnType.MODULE_ARRAY.isRefArray());
  }

  @Test
  void enumColumnTypeExistsWithStringBase() {
    assertEquals(ColumnType.STRING, ColumnType.ENUM.getBaseType());
    assertEquals(ColumnType.STRING_ARRAY, ColumnType.ENUM_ARRAY.getBaseType());
    assertTrue(ColumnType.ENUM.isEnum());
    assertTrue(ColumnType.ENUM_ARRAY.isEnum());
    assertFalse(ColumnType.ENUM.isReference());
    assertFalse(ColumnType.ENUM_ARRAY.isReference());
  }

  @Test
  void columnValuesRoundtrip() {
    Column col = Column.column("myEnum").setType(ColumnType.ENUM);
    assertNull(col.getValues());

    col.setValues("red", "green", "blue");
    assertEquals(List.of("red", "green", "blue"), col.getValues());

    col.setValues(List.of("alpha", "beta"));
    assertEquals(List.of("alpha", "beta"), col.getValues());
  }

  @Test
  void copiedColumnPreservesValues() {
    Column original = Column.column("myEnum").setType(ColumnType.ENUM);
    original.setValues("red", "green", "blue");

    Column copied = new Column(original);
    assertEquals(List.of("red", "green", "blue"), copied.getValues());
  }

  @Test
  void syncCopiesInheritNames() {
    SchemaMetadata source = new SchemaMetadata();
    TableMetadata original = table("Child");
    original.setInheritNames("Root");
    source.create(original);

    TableMetadata copy = new TableMetadata("Child");
    copy.sync(original);

    assertEquals(List.of("Root"), copy.getInheritNames());
  }

  @Test
  void clearCacheResetsInheritNames() {
    TableMetadata tableMetadata = table("Child");
    tableMetadata.setInheritNames("Root");
    tableMetadata.clearCache();

    assertTrue(tableMetadata.getInheritNames().isEmpty());
  }

  // C2: getAllInheritNames must not throw on diamond (A reachable via two paths)
  @Test
  void getAllInheritNamesDoesNotThrowOnDiamond() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table("A", column("id").setPkey()),
        table("B").setInheritNames("A"),
        table("C").setInheritNames("A"),
        table("D").setInheritNames(List.of("B", "C")));

    List<String> names = schema.getTableMetadata("D").getAllInheritNames();
    assertTrue(names.contains("A"), "Must contain A");
    assertTrue(names.contains("B"), "Must contain B");
    assertTrue(names.contains("C"), "Must contain C");
    assertTrue(names.contains("D"), "Must contain D");
    assertEquals(4, names.size(), "Must contain exactly 4 unique names (no duplicates)");
  }

  // C2: a genuine cycle must still throw
  @Test
  void getAllInheritNamesThrowsOnGenuineCycle() {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata x = table("X");
    TableMetadata y = table("Y");
    x.setInheritNames("Y");
    y.setInheritNames("X");
    schema.create(x, y);

    assertThrows(
        MolgenisException.class,
        () -> schema.getTableMetadata("X").getAllInheritNames(),
        "Cyclic inheritance must throw");
  }

  // S1: getSubclassTables must not return duplicates in a diamond
  @Test
  void getSubclassTablesDeduplicatesInDiamond() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table("A", column("id").setPkey()),
        table("B").setInheritNames("A"),
        table("C").setInheritNames("A"),
        table("D").setInheritNames(List.of("B", "C")));

    List<TableMetadata> subclasses = schema.getTableMetadata("A").getSubclassTables();
    long countD = subclasses.stream().filter(t -> t.getTableName().equals("D")).count();
    assertEquals(1, countD, "D must appear exactly once in A.getSubclassTables()");
  }

  // S1: getColumnsIncludingSubclasses must not return duplicate columns in a diamond
  @Test
  void getColumnsIncludingSubclassesDeduplicatesInDiamond() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table("A", column("id").setPkey()),
        table("B", column("bCol")).setInheritNames("A"),
        table("C", column("cCol")).setInheritNames("A"),
        table("D", column("dCol")).setInheritNames(List.of("B", "C")));

    List<Column> cols = schema.getTableMetadata("A").getColumnsIncludingSubclasses();
    long dColCount = cols.stream().filter(c -> c.getName().equals("dCol")).count();
    assertEquals(1, dColCount, "dCol must appear exactly once, not duplicated via B and C paths");
  }

  @Test
  void validateInheritanceDoesNotThrowOnValidDiamond() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table("A", column("id").setPkey()),
        table("B", column("bCol")).setInheritNames("A"),
        table("C", column("cCol")).setInheritNames("A"),
        table("D").setInheritNames(List.of("B", "C")));

    assertDoesNotThrow(() -> schema.getTableMetadata("D").validateInheritance());
  }

  @Test
  void validateInheritanceThrowsOnMultipleRoots() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table("Root1", column("id").setPkey()),
        table("Root2", column("id").setPkey()),
        table("B").setInheritNames("Root1"),
        table("C").setInheritNames("Root2"),
        table("D").setInheritNames(List.of("B", "C")));

    assertThrows(MolgenisException.class, () -> schema.getTableMetadata("D").validateInheritance());
  }

  @Test
  void validateInheritanceThrowsOnCycle() {
    SchemaMetadata schema = new SchemaMetadata();
    TableMetadata x = table("X");
    TableMetadata y = table("Y");
    x.setInheritNames("Y");
    y.setInheritNames("X");
    schema.create(x, y);

    assertThrows(MolgenisException.class, () -> schema.getTableMetadata("X").validateInheritance());
  }

  @Test
  void validateInheritanceThrowsOnCrossParentColumnCollision() {
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(
        table("Root", column("id").setPkey()),
        table("B", column("dup")).setInheritNames("Root"),
        table("C", column("dup")).setInheritNames("Root"),
        table("D").setInheritNames(List.of("B", "C")));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class, () -> schema.getTableMetadata("D").validateInheritance());
    assertTrue(
        ex.getMessage().contains("dup"), "Exception message must mention the colliding column");
  }

  @Test
  void getDiscriminatorColumnsFindsModuleArrayColumns() {
    TableMetadata tableWithDiscriminators =
        table(
            "Host",
            column("id").setPkey(),
            column("panels").setType(ColumnType.MODULE_ARRAY),
            column("groups").setType(ColumnType.MODULE_ARRAY),
            column("label").setType(ColumnType.STRING),
            column("category").setType(ColumnType.ENUM));
    SchemaMetadata schema = new SchemaMetadata();
    schema.create(tableWithDiscriminators);

    TableMetadata host = schema.getTableMetadata("Host");

    List<Column> discriminators = host.getDiscriminatorColumns();
    assertEquals(2, discriminators.size(), "exactly the two MODULE_ARRAY columns");
    assertEquals("panels", discriminators.get(0).getName());
    assertEquals("groups", discriminators.get(1).getName());

    assertTrue(
        host.getColumn("panels").isModuleDiscriminator(),
        "MODULE_ARRAY column must be a discriminator");
    assertTrue(
        host.getColumn("groups").isModuleDiscriminator(),
        "MODULE_ARRAY column must be a discriminator");
    assertFalse(
        host.getColumn("label").isModuleDiscriminator(),
        "STRING column must not be a discriminator");
    assertFalse(
        host.getColumn("category").isModuleDiscriminator(),
        "ENUM column must not be a discriminator — isModuleDiscriminator is exact-type, not base-type");
    assertFalse(
        host.getColumn("id").isModuleDiscriminator(), "PK column must not be a discriminator");
  }
}
