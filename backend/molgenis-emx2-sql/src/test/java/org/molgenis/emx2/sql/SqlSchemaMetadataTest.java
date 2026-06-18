package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class SqlSchemaMetadataTest {

  @Test
  void givenAdminUser_whenRequestingInheritedRoles_thenReturnAllPrivileges() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(getClass().getSimpleName());
    database.becomeAdmin();

    List<String> expectedRoles =
        Arrays.stream(Privileges.values()).map(Privileges::toString).toList();
    assertEquals(expectedRoles, schema.getInheritedRolesForActiveUser());
  }

  @Test
  void copyConstructor_copiesNameAndDescription() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema("CopyConstructorNameTest");
    schema.getMetadata().setDescription("test description");
    SqlSchemaMetadata source = (SqlSchemaMetadata) schema.getMetadata();

    SqlSchemaMetadata copy = new SqlSchemaMetadata(db, source);

    assertEquals(source.getName(), copy.getName());
    assertEquals(source.getDescription(), copy.getDescription());
  }

  @Test
  void copyConstructor_copiesAllTables() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema("CopyConstructorTablesTest");
    schema.create(
        table("Person").add(column("id").setPkey()).add(column("name")),
        table("Order").add(column("id").setPkey()));
    SqlSchemaMetadata source = (SqlSchemaMetadata) schema.getMetadata();

    SqlSchemaMetadata copy = new SqlSchemaMetadata(db, source);

    assertEquals(source.getTableNames(), copy.getTableNames());
    assertNotNull(copy.getTableMetadata("Person"));
    assertNotNull(copy.getTableMetadata("Order"));
    assertNotNull(copy.getTableMetadata("Person").getColumn("name"));
  }

  @Test
  void copyConstructor_copiesSettings() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema("CopyConstructorSettingsTest");
    schema.getMetadata().setSettingsWithoutReload(Map.of("key1", "value1", "key2", "value2"));
    SqlSchemaMetadata source = (SqlSchemaMetadata) schema.getMetadata();

    SqlSchemaMetadata copy = new SqlSchemaMetadata(db, source);

    assertEquals(source.getSettings(), copy.getSettings());
  }

  @Test
  void copyConstructor_hasIndependentPermissionsCache() {
    Database db = TestDatabaseFactory.getTestDatabase();
    Schema schema = db.dropCreateSchema("CopyConstructorPermissionsTest");
    SqlSchemaMetadata source = (SqlSchemaMetadata) schema.getMetadata();

    SqlSchemaMetadata copy = new SqlSchemaMetadata(db, source);

    assertNotSame(source.getTablePermissions(), copy.getTablePermissions());
  }
}
