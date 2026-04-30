package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.junit.jupiter.api.Assertions.*;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.TablePermission;

class SqlRoleManagerWildcardIT {

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();

  private SqlRoleManager roleManager;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    roleManager = new SqlRoleManager(database);
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
  }

  @Test
  void wildcardMaterialisedOnExistingSchemas() {
    DSLContext jooq = database.getJooq();
    String schema1 = "rmt_wm_schema1";
    String schema2 = "rmt_wm_schema2";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schema1 + "\"");
    jooq.execute("CREATE SCHEMA \"" + schema2 + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schema1 + "\".studies (id int PRIMARY KEY)");
      jooq.execute("CREATE TABLE \"" + schema2 + "\".studies (id int PRIMARY KEY)");

      roleManager.createRole("rmt wm role", "wildcard materialisation test");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission("*", "*")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt wm role", ps);

      boolean grantOnSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt wm role"))
                          .and(field("table_schema").eq(inline(schema1)))
                          .and(field("table_name").eq(inline("studies")))
                          .and(field("privilege_type").eq(inline("SELECT")))));
      assertTrue(grantOnSchema1, "Wildcard *:* must materialise SELECT GRANT on schema1.studies");

      boolean grantOnSchema2 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt wm role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("studies")))
                          .and(field("privilege_type").eq(inline("SELECT")))));
      assertTrue(grantOnSchema2, "Wildcard *:* must materialise SELECT GRANT on schema2.studies");

      PermissionSet got = roleManager.getPermissions("rmt wm role");
      TablePermission schema1Perm = got.resolveFor(schema1, "studies");
      assertTrue(
          schema1Perm.select().contains(TablePermission.SelectScope.ALL),
          "Materialised grant must be present in getPermissions for schema1");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt wm role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt wm role\"");
    }
  }

  @Test
  void wildcardExistingAndFuture() {
    DSLContext jooq = database.getJooq();
    String schema1 = "rmt_wc_schema1";
    String schema2 = "rmt_wc_schema2";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schema1 + "\"");
    jooq.execute("CREATE SCHEMA \"" + schema2 + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schema1 + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("CREATE TABLE \"" + schema2 + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt wc role", "wildcard test");

      PermissionSet ps = new PermissionSet();
      ps.put(new TablePermission("*", "*").select(TablePermission.SelectScope.ALL));
      roleManager.setPermissions("rmt wc role", ps);

      boolean grantSchema1 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt wc role"))
                          .and(field("table_schema").eq(inline(schema1)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema1, "Wildcard *:* should materialise GRANT on schema1.t1");

      boolean grantSchema2 =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt wc role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(grantSchema2, "Wildcard *:* should materialise GRANT on schema2.t1");

      PermissionSet got = roleManager.getPermissions("rmt wc role");
      TablePermission schema1Perm = got.resolveFor(schema1, "t1");
      assertTrue(
          schema1Perm.select().contains(TablePermission.SelectScope.ALL),
          "Wildcard materialised grant should be visible for schema1.t1");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt wc role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt wc role\"");
    }
  }

  @Test
  void schemaDropNoError() {
    DSLContext jooq = database.getJooq();
    String schema1 = "rmt_nodrop_schema1";
    String schema2 = "rmt_nodrop_schema2";
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
    jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
    jooq.execute("CREATE SCHEMA \"" + schema1 + "\"");
    jooq.execute("CREATE SCHEMA \"" + schema2 + "\"");
    try {
      jooq.execute("CREATE TABLE \"" + schema1 + "\".t1 (id int PRIMARY KEY)");
      jooq.execute("CREATE TABLE \"" + schema2 + "\".t1 (id int PRIMARY KEY)");

      roleManager.createRole("rmt nodrop role", "no error on drop");

      PermissionSet ps = new PermissionSet();
      ps.put(
          new TablePermission("*", "*")
              .select(TablePermission.singletonSelect(TablePermission.SelectScope.ALL)));
      roleManager.setPermissions("rmt nodrop role", ps);

      jooq.execute("DROP SCHEMA \"" + schema1 + "\" CASCADE");

      assertDoesNotThrow(
          () -> roleManager.getPermissions("rmt nodrop role"),
          "getPermissions should not throw after schema with wildcard policies is dropped");

      boolean hasSchema2Grant =
          jooq.fetchExists(
              jooq.select()
                  .from("information_schema.role_table_grants")
                  .where(
                      field("grantee")
                          .eq(inline("MG_ROLE_rmt nodrop role"))
                          .and(field("table_schema").eq(inline(schema2)))
                          .and(field("table_name").eq(inline("t1")))));
      assertTrue(hasSchema2Grant, "schema2 grant should remain after schema1 is dropped");
    } finally {
      database.becomeAdmin();
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema1 + "\" CASCADE");
      jooq.execute("DROP SCHEMA IF EXISTS \"" + schema2 + "\" CASCADE");
      jooq.execute("DROP OWNED BY \"MG_ROLE_rmt nodrop role\"");
      jooq.execute("DROP ROLE IF EXISTS \"MG_ROLE_rmt nodrop role\"");
    }
  }
}
