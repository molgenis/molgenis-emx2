package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ProfileMetadataTest {

  @Test
  void testBlockTableTypeExists() {
    assertNotNull(TableType.BLOCK);
    assertEquals(3, TableType.values().length);
  }

  @Test
  void testProfileColumnType() {
    assertEquals(ColumnType.STRING, ColumnType.PROFILE.getBaseType());
    assertFalse(ColumnType.PROFILE.isReference());
    assertTrue(ColumnType.PROFILE.isAtomicType());
    assertFalse(ColumnType.PROFILE.isArray());
  }

  @Test
  void testProfilesColumnType() {
    assertEquals(ColumnType.STRING_ARRAY, ColumnType.PROFILES.getBaseType());
    assertTrue(ColumnType.PROFILES.isArray());
    assertFalse(ColumnType.PROFILES.isReference());
  }

  @Test
  void testSetInheritNameVarargs() {
    TableMetadata table = table("MyTable");
    table.setInheritNames("sampling", "sequencing");
    assertArrayEquals(new String[] {"sampling", "sequencing"}, table.getInheritNames());
  }

  @Test
  void testSetInheritNameSingleWraps() {
    TableMetadata table = table("MyTable");
    table.setInheritNames("Person");
    assertArrayEquals(new String[] {"Person"}, table.getInheritNames());
  }

  @Test
  void testSetInheritNameNull() {
    TableMetadata table = table("MyTable");
    assertNull(table.getInheritNames());
  }

  @Test
  void testGetInheritedTablesReturnsAllDirectParents() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata experiments = table("Experiments", column("id").setPkey());
    TableMetadata sampling =
        table("sampling").setTableType(TableType.BLOCK).setInheritNames("Experiments");
    TableMetadata wgs = table("WGS").setInheritNames("sampling");

    schema.create(experiments);
    schema.create(sampling);
    schema.create(wgs);

    List<TableMetadata> parents = wgs.getInheritedTables();
    assertNotNull(parents);
    assertEquals(1, parents.size());
    assertEquals("sampling", parents.get(0).getTableName());
  }

  @Test
  void testGetSubclassTablesDeduped() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata experiments = table("Experiments", column("id").setPkey());
    TableMetadata sampling =
        table("sampling").setTableType(TableType.BLOCK).setInheritNames("Experiments");
    TableMetadata sequencing =
        table("sequencing").setTableType(TableType.BLOCK).setInheritNames("Experiments");
    TableMetadata wgs = table("WGS").setInheritNames("sampling", "sequencing");
    TableMetadata imaging = table("Imaging").setInheritNames("Experiments");

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
  void testBlockTableType() {
    TableMetadata blockTable = table("SomeBlock").setTableType(TableType.BLOCK);
    assertEquals(TableType.BLOCK, blockTable.getTableType());

    TableMetadata dataTable = table("SomeData").setTableType(TableType.DATA);
    assertNotEquals(TableType.BLOCK, dataTable.getTableType());
  }

  @Test
  void testGetProfileColumnWithProfileType() {
    TableMetadata tableWithProfile =
        table("WithProfile", column("name"), column("myProfile").setType(ColumnType.PROFILE));
    Column profileColumn = tableWithProfile.getProfileColumn();
    assertNotNull(profileColumn);
    assertEquals("myProfile", profileColumn.getName());
  }

  @Test
  void testGetProfileColumnWithProfilesType() {
    TableMetadata tableWithProfiles =
        table("WithProfiles", column("name"), column("myProfiles").setType(ColumnType.PROFILES));
    Column profileColumn = tableWithProfiles.getProfileColumn();
    assertNotNull(profileColumn);
    assertEquals("myProfiles", profileColumn.getName());
  }

  @Test
  void testGetAllInheritNamesIncludesAllParentChains() {
    SchemaMetadata schema = new SchemaMetadata("test");
    TableMetadata experiments = table("Experiments", column("id").setPkey());
    TableMetadata sampling = table("sampling").setInheritNames("Experiments");
    TableMetadata sequencing = table("sequencing").setInheritNames("Experiments");
    TableMetadata wgs = table("WGS").setInheritNames("sampling", "sequencing");

    schema.create(experiments);
    schema.create(sampling);
    schema.create(sequencing);
    schema.create(wgs);

    List<String> allNames = wgs.getAllInheritNames();
    assertTrue(allNames.contains("WGS"), "Should contain self");
    assertTrue(allNames.contains("sampling"), "Should contain first parent");
    assertTrue(allNames.contains("sequencing"), "Should contain second parent");
    assertTrue(allNames.contains("Experiments"), "Should contain root ancestor");

    long distinctCount = allNames.stream().distinct().count();
    assertEquals(allNames.size(), distinctCount, "No duplicates expected (diamond pattern)");
  }
}
