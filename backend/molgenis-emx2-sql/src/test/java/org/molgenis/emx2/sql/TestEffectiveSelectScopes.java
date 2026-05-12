package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;

class TestEffectiveSelectScopes {

  private static final String SCHEMA_NAME = "ESSTest";
  private static final String TABLE_NAME = "Obs";
  private static final String CUSTOM_ROLE = "res";
  private static final String TEST_USER = "ESSUser";
  private static final String NO_ROLE_USER = "ESSNoRoleUser";

  private static final SqlDatabase db = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private static final SqlRoleManager roleManager = new SqlRoleManager(db);

  private Schema schema;
  private SqlTableMetadata tableMetadata;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schema = db.dropCreateSchema(SCHEMA_NAME);
    schema.create(table(TABLE_NAME, column("id").setPkey()));
    tableMetadata = db.getSchema(SCHEMA_NAME).getMetadata().getTableMetadata(TABLE_NAME);
    if (!db.hasUser(TEST_USER)) db.addUser(TEST_USER);
    if (!db.hasUser(NO_ROLE_USER)) db.addUser(NO_ROLE_USER);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_NAME);
  }

  @Test
  void customRole_withSelectOwn_returnsOwn() {
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, CUSTOM_ROLE, "role with OWN select");
    PermissionSet perms = new PermissionSet();
    perms.putTable(TABLE_NAME, new TablePermission(TABLE_NAME).select(SelectScope.OWN));
    roleManager.setPermissions(schema, CUSTOM_ROLE, perms);
    roleManager.grantRoleToUser(schema, CUSTOM_ROLE, TEST_USER);

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertEquals(Set.of(SelectScope.OWN), scopes);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRole_withSelectAll_returnsAll() {
    roleManager.createRole(schema, CUSTOM_ROLE, "role with ALL select");
    PermissionSet perms = new PermissionSet();
    perms.putTable(TABLE_NAME, new TablePermission(TABLE_NAME).select(SelectScope.ALL));
    roleManager.setPermissions(schema, CUSTOM_ROLE, perms);
    roleManager.grantRoleToUser(schema, CUSTOM_ROLE, TEST_USER);

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertEquals(Set.of(SelectScope.ALL), scopes);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRole_withSelectNone_returnsEmpty() {
    roleManager.createRole(schema, CUSTOM_ROLE, "role with NONE select");
    PermissionSet perms = new PermissionSet();
    perms.putTable(TABLE_NAME, new TablePermission(TABLE_NAME).select(SelectScope.NONE));
    roleManager.setPermissions(schema, CUSTOM_ROLE, perms);
    roleManager.grantRoleToUser(schema, CUSTOM_ROLE, TEST_USER);

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertTrue(scopes.isEmpty());
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void noCustomRole_systemRoleCount_returnsCount() {
    schema.addMember(TEST_USER, "Count");

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertEquals(Set.of(SelectScope.COUNT), scopes);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void noCustomRole_systemRoleAggregator_returnsAggregate() {
    schema.addMember(TEST_USER, "Aggregator");

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertEquals(Set.of(SelectScope.AGGREGATE), scopes);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void noCustomRole_systemRoleRange_returnsRange() {
    schema.addMember(TEST_USER, "Range");

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertEquals(Set.of(SelectScope.RANGE), scopes);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void noCustomRole_systemRoleExists_returnsExists() {
    schema.addMember(TEST_USER, "Exists");

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertEquals(Set.of(SelectScope.EXISTS), scopes);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void customRoleOwn_andSystemRoleCount_returnsBoth() {
    schema.addMember(TEST_USER, "Count");
    schema.getTable(TABLE_NAME).getMetadata().setRlsEnabled(true);
    roleManager.createRole(schema, CUSTOM_ROLE, "role with OWN select");
    PermissionSet perms = new PermissionSet();
    perms.putTable(TABLE_NAME, new TablePermission(TABLE_NAME).select(SelectScope.OWN));
    roleManager.setPermissions(schema, CUSTOM_ROLE, perms);
    roleManager.grantRoleToUser(schema, CUSTOM_ROLE, TEST_USER);

    db.setActiveUser(TEST_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertEquals(Set.of(SelectScope.OWN, SelectScope.COUNT), scopes);
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void userWithNoRole_returnsEmpty() {
    db.setActiveUser(NO_ROLE_USER);
    try {
      Set<SelectScope> scopes = roleManager.getEffectiveSelectScopes(schema, tableMetadata);
      assertTrue(scopes.isEmpty());
    } finally {
      db.becomeAdmin();
    }
  }
}
