package org.molgenis.emx2.sql.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
  private CachedUserPermissions userPermissions;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();

    if (!database.hasUser(USER)) {
      database.addUser(USER);
    }

    Schema schema = database.dropCreateSchema(SCHEMA);
    schema.create(
        table(TABLE_A).add(column("id").setPkey()), table(TABLE_B).add(column("id").setPkey()));

    schema.createRole("CacheTestRole");
    schema.grant("CacheTestRole", new TablePermission(TABLE_A).select(true));
    schema.addMember(USER, "CacheTestRole");
  }

  @BeforeEach
  void schemaMetadataFor() {
    database.becomeAdmin();
    database.setActiveUser(USER);

    SqlSchemaMetadata metadata = (SqlSchemaMetadata) database.getSchema(SCHEMA).getMetadata();
    userPermissions = new CachedUserPermissions(metadata);
  }

  @Test
  void getValue_returnsPermissionsForActiveUser() {
    Map<String, TablePermission> permissions = userPermissions.getByTable();

    assertTrue(permissions.containsKey(TABLE_A), "granted table should be present");
    assertTrue(permissions.get(TABLE_A).hasSelect(), "granted select should be true");
  }

  @Test
  void getValue_onRepeatedCalls_returnsSameInstance() {
    Map<String, TablePermission> first = userPermissions.getByTable();
    Map<String, TablePermission> second = userPermissions.getByTable();

    assertSame(first, second, "repeated calls should return the cached map instance");
  }

  @Test
  void getValue_afterReset_returnsFreshInstance() {
    Map<String, TablePermission> before = userPermissions.getByTable();

    userPermissions.clearCache();

    Map<String, TablePermission> after = userPermissions.getByTable();
    assertNotSame(before, after, "map should be a new instance after cache reset via reload()");
  }

  @Test
  void getValue_returnsUnmodifiableMap() {
    Map<String, TablePermission> permissions = userPermissions.getByTable();

    TablePermission permission = new TablePermission("any");
    assertThrows(UnsupportedOperationException.class, () -> permissions.put("any", permission));
  }

  @Test
  void getValue_tableWithoutGrant_isAbsentFromCache() {
    Map<String, TablePermission> permissions = userPermissions.getByTable();

    assertFalse(permissions.containsKey(TABLE_B), "non-granted table should not appear in cache");
  }
}
