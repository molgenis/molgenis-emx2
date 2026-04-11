package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ProfileMetadataTest {

  @Test
  void testInternalTableTypeExists() {
    assertNotNull(TableType.INTERNAL);
    assertEquals(3, TableType.values().length);
  }

  @Test
  void testProfileColumnType() {
    assertEquals(ColumnType.STRING, ColumnType.VARIANT.getBaseType());
    assertFalse(ColumnType.VARIANT.isReference());
    assertTrue(ColumnType.VARIANT.isAtomicType());
    assertFalse(ColumnType.VARIANT.isArray());
  }

  @Test
  void testProfilesColumnType() {
    assertEquals(ColumnType.STRING_ARRAY, ColumnType.VARIANT_ARRAY.getBaseType());
    assertTrue(ColumnType.VARIANT_ARRAY.isArray());
    assertFalse(ColumnType.VARIANT_ARRAY.isReference());
  }

  @Test
  void testSetExtendNameVarargs() {
    TableMetadata table = table("MyTable");
    table.setExtendNames("sampling", "sequencing");
    assertArrayEquals(new String[] {"sampling", "sequencing"}, table.getExtendNames());
  }

  @Test
  void testSetExtendNameSingleWraps() {
    TableMetadata table = table("MyTable");
    table.setExtendNames("Person");
    assertArrayEquals(new String[] {"Person"}, table.getExtendNames());
  }

  @Test
  void testSetExtendNameNull() {
    TableMetadata table = table("MyTable");
    assertNull(table.getExtendNames());
  }

  @Test
  void testGetExtendedTablesReturnsAllDirectParents() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata experiments = table("Experiments", column("id").setPkey());
    TableMetadata sampling =
        table("sampling").setTableType(TableType.INTERNAL).setExtendNames("Experiments");
    TableMetadata wgs = table("WGS").setExtendNames("sampling");

    schema.create(experiments);
    schema.create(sampling);
    schema.create(wgs);

    List<TableMetadata> parents = wgs.getExtendedTables();
    assertNotNull(parents);
    assertEquals(1, parents.size());
    assertEquals("sampling", parents.get(0).getTableName());
  }

  @Test
  void testGetSubclassTablesDeduped() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata experiments = table("Experiments", column("id").setPkey());
    TableMetadata sampling =
        table("sampling").setTableType(TableType.INTERNAL).setExtendNames("Experiments");
    TableMetadata sequencing =
        table("sequencing").setTableType(TableType.INTERNAL).setExtendNames("Experiments");
    TableMetadata wgs = table("WGS").setExtendNames("sampling", "sequencing");
    TableMetadata imaging = table("Imaging").setExtendNames("Experiments");

    schema.create(experiments);
    schema.create(sampling);
    schema.create(sequencing);
    schema.create(wgs);
    schema.create(imaging);

    List<TableMetadata> subclasses = experiments.getSubclassTables();
    assertNotNull(subclasses);

    List<String> names = subclasses.stream().map(TableMetadata::getTableName).toList();
    assertTrue(names.contains("sampling"), "Expected sampling in subclasses");
    assertTrue(names.contains("sequencing"), "Expected sequencing in subclasses");
    assertTrue(names.contains("WGS"), "Expected WGS in subclasses");
    assertTrue(names.contains("Imaging"), "Expected Imaging in subclasses");

    long distinctCount = names.stream().distinct().count();
    assertEquals(names.size(), distinctCount, "Expected no duplicates in subclass list");
  }

  @Test
  void testInternalTableType() {
    TableMetadata internalTable = table("SomeInternal").setTableType(TableType.INTERNAL);
    assertEquals(TableType.INTERNAL, internalTable.getTableType());

    TableMetadata dataTable = table("SomeData").setTableType(TableType.DATA);
    assertNotEquals(TableType.INTERNAL, dataTable.getTableType());
  }

  @Test
  void testGetProfileColumnWithProfileType() {
    TableMetadata tableWithProfile =
        table("WithProfile", column("name"), column("myProfile").setType(ColumnType.VARIANT));
    Column profileColumn = tableWithProfile.getProfileColumn();
    assertNotNull(profileColumn);
    assertEquals("myProfile", profileColumn.getName());
  }

  @Test
  void testGetProfileColumnWithProfilesType() {
    TableMetadata tableWithProfiles =
        table(
            "WithProfiles", column("name"), column("myProfiles").setType(ColumnType.VARIANT_ARRAY));
    Column profileColumn = tableWithProfiles.getProfileColumn();
    assertNotNull(profileColumn);
    assertEquals("myProfiles", profileColumn.getName());
  }

  @Test
  void testGetColumnsForSubsets_noActiveSubsets_returnsAll() {
    TableMetadata table =
        table(
            "MyTable",
            column("id").setPkey(),
            column("wgsOnly").setProfiles("wgs"),
            column("always"));
    List<Column> result = table.getColumnsForProfiles(null);
    assertEquals(3, result.size());
  }

  @Test
  void testGetColumnsForSubsets_withActiveSubset_filtersCorrectly() {
    TableMetadata table =
        table(
            "MyTable",
            column("id").setPkey(),
            column("wgsOnly").setProfiles("wgs"),
            column("rdmOnly").setProfiles("rdm"),
            column("always"));
    List<Column> result = table.getColumnsForProfiles(new String[] {"wgs"});
    List<String> names = result.stream().map(Column::getName).toList();
    assertTrue(names.contains("id"));
    assertTrue(names.contains("wgsOnly"));
    assertTrue(names.contains("always"));
    assertFalse(names.contains("rdmOnly"));
  }

  @Test
  void testGetNonInheritedColumnsForSubsets_filtersCorrectly() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata base = table("Base", column("id").setPkey(), column("baseCol"));
    TableMetadata child =
        table(
            "Child", column("childWgs").setProfiles("wgs"), column("childRdm").setProfiles("rdm"));
    child.setExtendNames("Base");
    schema.create(base);
    schema.create(child);

    List<Column> result = child.getNonInheritedColumnsForProfiles(new String[] {"wgs"});
    List<String> names = result.stream().map(Column::getName).toList();
    assertTrue(names.contains("childWgs"));
    assertFalse(names.contains("childRdm"));
    assertFalse(names.contains("id"));
    assertFalse(names.contains("baseCol"));
  }

  @Test
  void testGetAllInheritNamesIncludesAllParentChains() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata experiments = table("Experiments", column("id").setPkey());
    TableMetadata sampling = table("sampling").setExtendNames("Experiments");
    TableMetadata sequencing = table("sequencing").setExtendNames("Experiments");
    TableMetadata wgs = table("WGS").setExtendNames("sampling", "sequencing");

    schema.create(experiments);
    schema.create(sampling);
    schema.create(sequencing);
    schema.create(wgs);

    List<String> allNames = wgs.getAllExtendNames();
    assertTrue(allNames.contains("WGS"), "Should contain self");
    assertTrue(allNames.contains("sampling"), "Should contain first parent");
    assertTrue(allNames.contains("sequencing"), "Should contain second parent");
    assertTrue(allNames.contains("Experiments"), "Should contain root ancestor");

    long distinctCount = allNames.stream().distinct().count();
    assertEquals(allNames.size(), distinctCount, "No duplicates expected (diamond pattern)");
  }
}
