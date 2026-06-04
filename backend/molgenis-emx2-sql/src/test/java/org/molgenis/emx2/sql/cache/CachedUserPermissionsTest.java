package org.molgenis.emx2.sql.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlSchemaMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class CachedUserPermissionsTest {

  private static final String SCHEMA = "TablePermissionCacheTest";
  private static final String TABLE_A = "TableA";
  private static final String TABLE_B = "TableB";
  private static final String USER = "tpc_user";

  private static Database database;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    if (!database.hasUser(USER)) database.addUser(USER);

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        table(TABLE_A).add(column("id").setPkey()), table(TABLE_B).add(column("id").setPkey()));

    schema.createRole("CacheTestRole");
    schema.grant("CacheTestRole", new TablePermission(TABLE_A).select(true));
    schema.addMember(USER, "CacheTestRole");
  }

  private SqlSchemaMetadata schemaMetadataFor(String user) {
    database.becomeAdmin();
    database.setActiveUser(user);
    return (SqlSchemaMetadata) database.getSchema(SCHEMA).getMetadata();
  }

  @Test
  void getValue_returnsPermissionsForActiveUser() {
    SqlSchemaMetadata metadata = schemaMetadataFor(USER);

    Map<String, TablePermission> permissions = metadata.getPermissionsByTableForActiveUser();

    assertTrue(permissions.containsKey(TABLE_A), "granted table should be present");
    assertTrue(permissions.get(TABLE_A).hasSelect(), "granted select should be true");
  }

  @Test
  void getValue_onRepeatedCalls_returnsSameInstance() {
    SqlSchemaMetadata metadata = schemaMetadataFor(USER);

    Map<String, TablePermission> first = metadata.getPermissionsByTableForActiveUser();
    Map<String, TablePermission> second = metadata.getPermissionsByTableForActiveUser();

    assertSame(first, second, "repeated calls should return the cached map instance");
  }

  @Test
  void getValue_afterReset_returnsFreshInstance() {
    SqlSchemaMetadata metadata = schemaMetadataFor(USER);
    Map<String, TablePermission> before = metadata.getPermissionsByTableForActiveUser();

    metadata.reload();

    Map<String, TablePermission> after = metadata.getPermissionsByTableForActiveUser();
    assertNotSame(before, after, "map should be a new instance after cache reset via reload()");
  }

  @Test
  void getValue_returnsUnmodifiableMap() {
    SqlSchemaMetadata metadata = schemaMetadataFor(USER);
    Map<String, TablePermission> permissions = metadata.getPermissionsByTableForActiveUser();

    TablePermission permission = new TablePermission("any");
    assertThrows(UnsupportedOperationException.class, () -> permissions.put("any", permission));
  }

  @Test
  void getValue_tableWithoutGrant_isAbsentFromCache() {
    SqlSchemaMetadata metadata = schemaMetadataFor(USER);

    Map<String, TablePermission> permissions = metadata.getPermissionsByTableForActiveUser();

    assertFalse(permissions.containsKey(TABLE_B), "non-granted table should not appear in cache");
  }
}
