package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlRoleManager {
  private static final Logger logger = LoggerFactory.getLogger(SqlRoleManager.class);

  private static final Table<?> RLS_PERMISSIONS_TABLE = table(name("MOLGENIS", "rls_permissions"));
  private static final Field<String> RP_ROLE = field(name("role_name"), VARCHAR);
  private static final Field<String> RP_SCHEMA = field(name("table_schema"), VARCHAR);
  private static final Field<String> RP_TABLE = field(name("table_name"), VARCHAR);
  private static final Field<String> RP_SELECT_LEVEL = field(name("select_level"), VARCHAR);
  private static final Field<Boolean> RP_INSERT_RLS = field(name("insert_rls"), BOOLEAN);
  private static final Field<Boolean> RP_UPDATE_RLS = field(name("update_rls"), BOOLEAN);
  private static final Field<Boolean> RP_DELETE_RLS = field(name("delete_rls"), BOOLEAN);
  private static final Field<Boolean> RP_GRANT_PERMISSION =
      field(name("grant_permission"), BOOLEAN);
  private static final Field<String[]> RP_EDITABLE_COLUMNS =
      field(name("editable_columns"), VARCHAR.getArrayDataType());
  private static final Field<String[]> RP_READONLY_COLUMNS =
      field(name("readonly_columns"), VARCHAR.getArrayDataType());
  private static final Field<String[]> RP_HIDDEN_COLUMNS =
      field(name("hidden_columns"), VARCHAR.getArrayDataType());

  private final SqlDatabase database;

  public SqlRoleManager(SqlDatabase database) {
    this.database = database;
  }

  private DSLContext jooq() {
    return database.getJooq();
  }

  public void createRole(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot create system role: " + roleName);
    }
    String fullRole = fullRoleName(schemaName, roleName);
    String existsRole = fullRoleName(schemaName, Privileges.EXISTS.toString());
    executeCreateRole(jooq(), fullRole);
    // every custom role inherits the Exists role so it has schema USAGE
    jooq().execute("GRANT {0} TO {1}", name(existsRole), name(fullRole));
    jooq().execute("GRANT {0} TO session_user WITH ADMIN OPTION", name(fullRole));
  }

  public void deleteRole(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot delete system role: " + roleName);
    }
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    String fullRole = fullRoleName(schemaName, roleName);

    // revoke table grants and clean mg_roles arrays
    for (String tableName : database.getSchema(schemaName).getTableNames()) {
      Table<?> jooqTable = table(name(schemaName, tableName));
      jooq().execute("REVOKE ALL ON {0} FROM {1}", jooqTable, name(fullRole));

      if (hasMgRolesColumn(schemaName, tableName)) {
        jooq()
            .execute(
                "UPDATE {0} SET {1} = array_remove({1}, {2}) WHERE {1} @> ARRAY[{2}]",
                jooqTable, name(MG_ROLES), inline(fullRole));
      }
    }

    // clean rls_permissions
    jooq()
        .deleteFrom(RLS_PERMISSIONS_TABLE)
        .where(RP_SCHEMA.eq(schemaName))
        .and(RP_ROLE.eq(fullRole))
        .execute();

    revokeAllMemberships(fullRole);
    jooq().execute("DROP ROLE IF EXISTS {0}", name(fullRole));
    database.getListener().schemaChanged(schemaName);
  }

  public boolean roleExists(String schemaName, String roleName) {
    String fullRole = fullRoleName(schemaName, roleName);
    Integer count =
        jooq()
            .selectCount()
            .from("pg_roles")
            .where(field("rolname").eq(inline(fullRole)))
            .fetchOne(0, Integer.class);
    return count != null && count > 0;
  }

  // ── Member management ───────────────────────────────────────────────────────

  public void addMember(String schemaName, String roleName, String userName) {
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    if (!database.hasUser(userName)) {
      database.addUser(userName);
    }
    String fullRole = fullRoleName(schemaName, roleName);
    jooq().execute("GRANT {0} TO {1}", name(fullRole), name(MG_USER_PREFIX + userName));
  }

  public void removeMember(String schemaName, String roleName, String userName) {
    String fullRole = fullRoleName(schemaName, roleName);
    jooq().execute("REVOKE {0} FROM {1}", name(fullRole), name(MG_USER_PREFIX + userName));
  }

  // ── Grant / revoke permissions ──────────────────────────────────────────────

  public void grant(String schemaName, String roleName, Permission permission) {
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    String tableName = permission.getTable();
    if (tableName != null
        && !"*".equals(tableName)
        && !database.getSchema(schemaName).getTableNames().contains(tableName)) {
      throw new MolgenisException("Table does not exist: " + tableName);
    }

    String fullRole = fullRoleName(schemaName, roleName);

    if ("*".equals(tableName)) {
      syncRlsPermissions(schemaName, fullRole, "*", permission);
      for (String table : database.getSchema(schemaName).getTableNames()) {
        syncPermissionGrants(schemaName, fullRole, table, permission);
        if (permission.hasRowLevelPermissions()) {
          enableRowLevelSecurity(schemaName, table);
        }
      }
    } else if (tableName != null) {
      syncPermissionGrants(schemaName, fullRole, tableName, permission);
      syncRlsPermissions(schemaName, fullRole, tableName, permission);
      if (permission.hasRowLevelPermissions()) {
        enableRowLevelSecurity(schemaName, tableName);
      }
    }
    database.getListener().schemaChanged(schemaName);
  }

  public void revoke(String schemaName, String roleName, Permission permission) {
    String fullRole = fullRoleName(schemaName, roleName);
    String tableName = permission.getTable();

    if (tableName == null) {
      // revoke from all tables
      for (String table : database.getSchema(schemaName).getTableNames()) {
        revokeTableGrants(schemaName, fullRole, table, permission);
      }
      try {
        jooq()
            .deleteFrom(RLS_PERMISSIONS_TABLE)
            .where(RP_SCHEMA.eq(schemaName))
            .and(RP_ROLE.eq(fullRole))
            .execute();
      } catch (Exception e) {
        logger.error("Failed to revoke all permissions for {} in {}", fullRole, schemaName, e);
      }
    } else {
      String targetTable = "*".equals(tableName) ? "*" : tableName;
      if (!"*".equals(tableName)) {
        revokeTableGrants(schemaName, fullRole, tableName, permission);
      } else {
        for (String table : database.getSchema(schemaName).getTableNames()) {
          revokeTableGrants(schemaName, fullRole, table, permission);
        }
      }
      revokeRlsPermissions(schemaName, fullRole, targetTable, permission);
    }
    database.getListener().schemaChanged(schemaName);
  }

  // ── RLS management ──────────────────────────────────────────────────────────

  public void enableRowLevelSecurity(String schemaName, String tableName) {
    DSLContext ctx = jooq();
    Table<?> jooqTable = table(name(schemaName, tableName));

    // idempotent: skip if mg_roles column already exists
    if (hasMgRolesColumn(schemaName, tableName)) {
      return;
    }

    ctx.execute("ALTER TABLE {0} ADD COLUMN {1} TEXT[] DEFAULT NULL", jooqTable, name(MG_ROLES));
    ctx.execute(
        "CREATE INDEX IF NOT EXISTS {0} ON {1} USING GIN({2})",
        name(tableName + "_" + MG_ROLES + "_idx"), jooqTable, name(MG_ROLES));
    ctx.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", jooqTable);

    String fqTable = schemaName + "." + tableName;

    // drop any existing policies first
    for (String op : List.of("select", "insert", "update", "delete", "modify")) {
      ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_" + op), jooqTable);
    }

    // SELECT policy: pass if table not restricted OR row is public OR user's role matches
    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING ("
            + "{2} != ALL(string_to_array(COALESCE(current_setting({3}, true), ''), ',')) "
            + "OR {4} IS NULL "
            + "OR {4} && string_to_array(COALESCE(current_setting({5}, true), ''), ',')"
            + ")",
        name(tableName + "_rls_select"),
        jooqTable,
        inline(fqTable),
        inline(RLS_SELECT_TABLES),
        name(MG_ROLES),
        inline(RLS_ACTIVE_ROLE));

    // INSERT policy
    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR INSERT WITH CHECK ("
            + "{2} != ALL(string_to_array(COALESCE(current_setting({3}, true), ''), ',')) "
            + "OR {4} IS NULL "
            + "OR {4} && string_to_array(COALESCE(current_setting({5}, true), ''), ',')"
            + ")",
        name(tableName + "_rls_insert"),
        jooqTable,
        inline(fqTable),
        inline(RLS_INSERT_TABLES),
        name(MG_ROLES),
        inline(RLS_ACTIVE_ROLE));

    // UPDATE policy (needs both USING and WITH CHECK)
    String updateExpr =
        "{2} != ALL(string_to_array(COALESCE(current_setting({3}, true), ''), ',')) "
            + "OR {4} IS NULL "
            + "OR {4} && string_to_array(COALESCE(current_setting({5}, true), ''), ',')";
    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR UPDATE USING ("
            + updateExpr
            + ") WITH CHECK ("
            + updateExpr
            + ")",
        name(tableName + "_rls_update"),
        jooqTable,
        inline(fqTable),
        inline(RLS_UPDATE_TABLES),
        name(MG_ROLES),
        inline(RLS_ACTIVE_ROLE));

    // DELETE policy
    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR DELETE USING ("
            + "{2} != ALL(string_to_array(COALESCE(current_setting({3}, true), ''), ',')) "
            + "OR {4} IS NULL "
            + "OR {4} && string_to_array(COALESCE(current_setting({5}, true), ''), ',')"
            + ")",
        name(tableName + "_rls_delete"),
        jooqTable,
        inline(fqTable),
        inline(RLS_DELETE_TABLES),
        name(MG_ROLES),
        inline(RLS_ACTIVE_ROLE));

    applySchemaWideGrantsForNewTable(schemaName, tableName);
  }

  public void disableRowLevelSecurity(String schemaName, String tableName) {
    DSLContext ctx = jooq();
    Table<?> jooqTable = table(name(schemaName, tableName));
    for (String op : List.of("select", "insert", "update", "delete", "modify")) {
      ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_" + op), jooqTable);
    }
    ctx.execute("ALTER TABLE {0} DISABLE ROW LEVEL SECURITY", jooqTable);
  }

  public void applySchemaWideGrantsForNewTable(String schemaName, String tableName) {
    try {
      Result<?> wildcardPerms =
          jooq()
              .selectFrom(RLS_PERMISSIONS_TABLE)
              .where(RP_SCHEMA.eq(schemaName))
              .and(RP_TABLE.eq("*"))
              .fetch();

      if (wildcardPerms.isEmpty()) {
        return;
      }

      Table<?> jooqTable = table(name(schemaName, tableName));
      for (org.jooq.Record perm : wildcardPerms) {
        String fullRole = perm.get(RP_ROLE);
        if (perm.get(RP_SELECT_LEVEL) != null) {
          jooq().execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
        }
        if (perm.get(RP_INSERT_RLS) != null) {
          jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
        }
        if (perm.get(RP_UPDATE_RLS) != null) {
          jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
        }
        if (perm.get(RP_DELETE_RLS) != null) {
          jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
        }
      }
    } catch (Exception e) {
      logger.error(
          "Failed to apply schema-wide grants for new table {}.{}", schemaName, tableName, e);
    }
  }

  // ── Query role info and permissions ─────────────────────────────────────────

  public RoleInfo getRoleInfo(String schemaName, String roleName) {
    return new RoleInfo(
        roleName,
        getDescription(schemaName, roleName),
        isSystemRole(roleName),
        getPermissions(schemaName, roleName));
  }

  public List<RoleInfo> getRoleInfos(String schemaName) {
    List<RoleInfo> roleInfos = new ArrayList<>();
    for (String roleName : getRolesForSchema(schemaName)) {
      roleInfos.add(getRoleInfo(schemaName, roleName));
    }
    return roleInfos;
  }

  public List<Permission> getPermissions(String schemaName, String roleName) {
    String fullRole = fullRoleName(schemaName, roleName);
    List<Permission> permissions = new ArrayList<>();

    try {
      String sql = loadResource("permissions_query.sql");
      sql = sql.replace(":role", "{0}").replace(":schema", "{1}");
      Result<Record> result = jooq().fetch(sql, inline(fullRole), inline(schemaName));

      for (Record record : result) {
        Permission p = new Permission();
        p.setTable(record.get("table_name", String.class));

        Boolean canSelect = record.get("can_select", Boolean.class);
        String selectLevel = record.get("select_level", String.class);
        p.setSelect(mapSelectLevel(canSelect, selectLevel));

        Boolean canInsert = record.get("can_insert", Boolean.class);
        Boolean insertRls = record.get("insert_rls", Boolean.class);
        p.setInsert(mapModifyLevel(canInsert, insertRls));

        Boolean canUpdate = record.get("can_update", Boolean.class);
        Boolean updateRls = record.get("update_rls", Boolean.class);
        p.setUpdate(mapModifyLevel(canUpdate, updateRls));

        Boolean canDelete = record.get("can_delete", Boolean.class);
        Boolean deleteRls = record.get("delete_rls", Boolean.class);
        p.setDelete(mapModifyLevel(canDelete, deleteRls));

        Boolean grantPerm = record.get("grant_permission", Boolean.class);
        if (Boolean.TRUE.equals(grantPerm)) {
          p.setGrant(true);
        }

        p.setColumnAccess(readColumnAccess(record));
        permissions.add(p);
      }

      // also read wildcard (*) entry if it exists
      Record wildcardRecord =
          jooq()
              .selectFrom(RLS_PERMISSIONS_TABLE)
              .where(RP_ROLE.eq(fullRole))
              .and(RP_SCHEMA.eq(schemaName))
              .and(RP_TABLE.eq("*"))
              .fetchOne();
      if (wildcardRecord != null) {
        permissions.add(readWildcardPermission(wildcardRecord));
      }
    } catch (Exception e) {
      logger.error("Failed to get permissions for {} in {}", roleName, schemaName, e);
    }

    return permissions;
  }

  public List<Permission> getPermissionsForActiveUser(String schemaName) {
    String activeUser = database.getActiveUser();
    if (activeUser == null || "anonymous".equals(activeUser)) {
      return Collections.emptyList();
    }
    SqlSchema schema = database.getSchema(schemaName);
    if (schema == null) {
      return Collections.emptyList();
    }
    String roleName = schema.getRoleForUser(activeUser);
    if (roleName == null || roleName.isEmpty()) {
      return Collections.emptyList();
    }
    if (isSystemRole(roleName)) {
      return getSystemRolePermissions(schema, roleName);
    }
    return getPermissions(schemaName, roleName);
  }

  public List<String> getRolesForSchema(String schemaName) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    Result<Record> result =
        jooq()
            .select()
            .from("pg_roles")
            .where(field("rolname").like(inline(rolePrefix + "%")))
            .fetch();
    List<String> roles = new ArrayList<>();
    for (Record record : result) {
      String fullRole = record.get("rolname", String.class);
      roles.add(fullRole.substring(rolePrefix.length()));
    }
    return roles;
  }

  // ── Role description ────────────────────────────────────────────────────────

  public void setDescription(String schemaName, String roleName, String description) {
    String fullRole = fullRoleName(schemaName, roleName);
    jooq().execute("COMMENT ON ROLE {0} IS {1}", name(fullRole), inline(description));
  }

  public String getDescription(String schemaName, String roleName) {
    String fullRole = fullRoleName(schemaName, roleName);
    return jooq()
        .select(field("shobj_description(oid, 'pg_authid')"))
        .from("pg_roles")
        .where(field("rolname").eq(inline(fullRole)))
        .fetchOne(0, String.class);
  }

  // ── Cleanup ─────────────────────────────────────────────────────────────────

  public void cleanupTablePermissions(String schemaName, String tableName) {
    try {
      jooq()
          .deleteFrom(RLS_PERMISSIONS_TABLE)
          .where(RP_SCHEMA.eq(schemaName))
          .and(RP_TABLE.eq(tableName))
          .execute();
    } catch (Exception e) {
      logger.error("Failed to cleanup table permissions for {}.{}", schemaName, tableName, e);
    }
  }

  public void cleanupSchemaPermissions(String schemaName) {
    try {
      jooq().deleteFrom(RLS_PERMISSIONS_TABLE).where(RP_SCHEMA.eq(schemaName)).execute();
    } catch (Exception e) {
      logger.error("Failed to cleanup schema permissions for {}", schemaName, e);
    }
  }

  // ── Utility methods ─────────────────────────────────────────────────────────

  public boolean isSystemRole(String roleName) {
    return SYSTEM_ROLES.contains(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  public static boolean isGlobalRole(String fullRoleName) {
    return fullRoleName != null && fullRoleName.startsWith(MG_ROLE_PREFIX + GLOBAL_SCHEMA + "/");
  }

  // ── Private helpers ─────────────────────────────────────────────────────────

  private void syncPermissionGrants(
      String schemaName, String fullRole, String tableName, Permission permission) {
    Table<?> jooqTable = table(name(schemaName, tableName));
    SelectLevel select = permission.getSelect();
    if (select == SelectLevel.TABLE || select == SelectLevel.ROW) {
      jooq().execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
    }
    if (permission.getInsert() != null) {
      jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
    }
    if (permission.getUpdate() != null) {
      jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
    }
    if (permission.getDelete() != null) {
      jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
    }
  }

  private void syncRlsPermissions(
      String schemaName, String fullRole, String tableName, Permission permission) {
    String selectLevel = permission.getSelect() != null ? permission.getSelect().name() : null;
    Boolean insertRls = toRlsFlag(permission.getInsert());
    Boolean updateRls = toRlsFlag(permission.getUpdate());
    Boolean deleteRls = toRlsFlag(permission.getDelete());
    Boolean grantPerm = permission.getGrant();

    String[] editableCols = null;
    String[] readonlyCols = null;
    String[] hiddenCols = null;
    ColumnAccess ca = permission.getColumnAccess();
    if (ca != null) {
      editableCols = ca.editable() != null ? ca.editable().toArray(new String[0]) : null;
      readonlyCols = ca.readonly() != null ? ca.readonly().toArray(new String[0]) : null;
      hiddenCols = ca.hidden() != null ? ca.hidden().toArray(new String[0]) : null;
    }

    boolean hasAnyValue =
        selectLevel != null
            || insertRls != null
            || updateRls != null
            || deleteRls != null
            || grantPerm != null
            || editableCols != null
            || readonlyCols != null
            || hiddenCols != null;
    if (!hasAnyValue) {
      return;
    }

    // upsert with COALESCE merge semantics: non-null values overwrite, null values preserve
    jooq()
        .insertInto(RLS_PERMISSIONS_TABLE)
        .set(RP_ROLE, fullRole)
        .set(RP_SCHEMA, schemaName)
        .set(RP_TABLE, tableName)
        .set(RP_SELECT_LEVEL, selectLevel)
        .set(RP_INSERT_RLS, insertRls)
        .set(RP_UPDATE_RLS, updateRls)
        .set(RP_DELETE_RLS, deleteRls)
        .set(RP_GRANT_PERMISSION, grantPerm)
        .set(RP_EDITABLE_COLUMNS, editableCols)
        .set(RP_READONLY_COLUMNS, readonlyCols)
        .set(RP_HIDDEN_COLUMNS, hiddenCols)
        .onConflict(RP_SCHEMA, RP_ROLE, RP_TABLE)
        .doUpdate()
        .set(
            RP_SELECT_LEVEL,
            field(
                "COALESCE(EXCLUDED.select_level, {0}.select_level)",
                VARCHAR, RLS_PERMISSIONS_TABLE))
        .set(
            RP_INSERT_RLS,
            field("COALESCE(EXCLUDED.insert_rls, {0}.insert_rls)", BOOLEAN, RLS_PERMISSIONS_TABLE))
        .set(
            RP_UPDATE_RLS,
            field("COALESCE(EXCLUDED.update_rls, {0}.update_rls)", BOOLEAN, RLS_PERMISSIONS_TABLE))
        .set(
            RP_DELETE_RLS,
            field("COALESCE(EXCLUDED.delete_rls, {0}.delete_rls)", BOOLEAN, RLS_PERMISSIONS_TABLE))
        .set(
            RP_GRANT_PERMISSION,
            field(
                "COALESCE(EXCLUDED.grant_permission, {0}.grant_permission)",
                BOOLEAN, RLS_PERMISSIONS_TABLE))
        .set(
            RP_EDITABLE_COLUMNS,
            field(
                "COALESCE(EXCLUDED.editable_columns, {0}.editable_columns)",
                VARCHAR.getArrayDataType(), RLS_PERMISSIONS_TABLE))
        .set(
            RP_READONLY_COLUMNS,
            field(
                "COALESCE(EXCLUDED.readonly_columns, {0}.readonly_columns)",
                VARCHAR.getArrayDataType(), RLS_PERMISSIONS_TABLE))
        .set(
            RP_HIDDEN_COLUMNS,
            field(
                "COALESCE(EXCLUDED.hidden_columns, {0}.hidden_columns)",
                VARCHAR.getArrayDataType(), RLS_PERMISSIONS_TABLE))
        .execute();
  }

  private void revokeTableGrants(
      String schemaName, String fullRole, String tableName, Permission permission) {
    Table<?> jooqTable = table(name(schemaName, tableName));
    if (permission.getSelect() != null) {
      jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (permission.getInsert() != null) {
      jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (permission.getUpdate() != null) {
      jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (permission.getDelete() != null) {
      jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    // if nothing specific, revoke all
    if (permission.getSelect() == null
        && permission.getInsert() == null
        && permission.getUpdate() == null
        && permission.getDelete() == null) {
      jooq().execute("REVOKE ALL ON {0} FROM {1}", jooqTable, name(fullRole));
    }
  }

  private void revokeRlsPermissions(
      String schemaName, String fullRole, String tableName, Permission permission) {
    try {
      boolean allSpecified =
          permission.getSelect() != null
              && permission.getInsert() != null
              && permission.getUpdate() != null
              && permission.getDelete() != null;
      boolean noneSpecified =
          permission.getSelect() == null
              && permission.getInsert() == null
              && permission.getUpdate() == null
              && permission.getDelete() == null;

      if (allSpecified || noneSpecified) {
        // delete the entire row
        jooq()
            .deleteFrom(RLS_PERMISSIONS_TABLE)
            .where(RP_SCHEMA.eq(schemaName))
            .and(RP_ROLE.eq(fullRole))
            .and(RP_TABLE.eq(tableName))
            .execute();
      } else {
        // null out specific columns
        if (permission.getSelect() != null) {
          jooq()
              .update(RLS_PERMISSIONS_TABLE)
              .set(RP_SELECT_LEVEL, (String) null)
              .where(RP_SCHEMA.eq(schemaName))
              .and(RP_ROLE.eq(fullRole))
              .and(RP_TABLE.eq(tableName))
              .execute();
        }
        if (permission.getInsert() != null) {
          jooq()
              .update(RLS_PERMISSIONS_TABLE)
              .set(RP_INSERT_RLS, (Boolean) null)
              .where(RP_SCHEMA.eq(schemaName))
              .and(RP_ROLE.eq(fullRole))
              .and(RP_TABLE.eq(tableName))
              .execute();
        }
        if (permission.getUpdate() != null) {
          jooq()
              .update(RLS_PERMISSIONS_TABLE)
              .set(RP_UPDATE_RLS, (Boolean) null)
              .where(RP_SCHEMA.eq(schemaName))
              .and(RP_ROLE.eq(fullRole))
              .and(RP_TABLE.eq(tableName))
              .execute();
        }
        if (permission.getDelete() != null) {
          jooq()
              .update(RLS_PERMISSIONS_TABLE)
              .set(RP_DELETE_RLS, (Boolean) null)
              .where(RP_SCHEMA.eq(schemaName))
              .and(RP_ROLE.eq(fullRole))
              .and(RP_TABLE.eq(tableName))
              .execute();
        }
        // clean up rows where all values are empty
        jooq()
            .deleteFrom(RLS_PERMISSIONS_TABLE)
            .where(RP_SCHEMA.eq(schemaName))
            .and(RP_ROLE.eq(fullRole))
            .and(RP_TABLE.eq(tableName))
            .and(RP_SELECT_LEVEL.isNull())
            .and(RP_INSERT_RLS.isNull().or(RP_INSERT_RLS.eq(false)))
            .and(RP_UPDATE_RLS.isNull().or(RP_UPDATE_RLS.eq(false)))
            .and(RP_DELETE_RLS.isNull().or(RP_DELETE_RLS.eq(false)))
            .and(RP_GRANT_PERMISSION.isNull().or(RP_GRANT_PERMISSION.eq(false)))
            .and(RP_EDITABLE_COLUMNS.isNull())
            .and(RP_READONLY_COLUMNS.isNull())
            .and(RP_HIDDEN_COLUMNS.isNull())
            .execute();
      }
    } catch (Exception e) {
      logger.error(
          "Failed to revoke RLS permissions for {} on {}.{}", fullRole, schemaName, tableName, e);
    }
  }

  private boolean hasMgRolesColumn(String schemaName, String tableName) {
    Integer count =
        jooq()
            .selectCount()
            .from("information_schema.columns")
            .where(field("table_schema").eq(inline(schemaName)))
            .and(field("table_name").eq(inline(tableName)))
            .and(field("column_name").eq(inline(MG_ROLES)))
            .fetchOne(0, Integer.class);
    return count != null && count > 0;
  }

  private List<Permission> getSystemRolePermissions(SqlSchema schema, String roleName) {
    List<Permission> permissions = new ArrayList<>();
    for (String tableName : schema.getTableNames()) {
      Permission perm = new Permission(tableName);
      switch (roleName) {
        case "Owner", "Manager", "Editor" -> {
          perm.setSelect(SelectLevel.TABLE);
          perm.setInsert(ModifyLevel.TABLE);
          perm.setUpdate(ModifyLevel.TABLE);
          perm.setDelete(ModifyLevel.TABLE);
        }
        case "Viewer" -> perm.setSelect(SelectLevel.TABLE);
        case "Count" -> perm.setSelect(SelectLevel.COUNT);
        case "Aggregator" -> perm.setSelect(SelectLevel.AGGREGATOR);
        case "Range" -> perm.setSelect(SelectLevel.RANGE);
        case "Exists" -> perm.setSelect(SelectLevel.EXISTS);
        default -> {}
      }
      permissions.add(perm);
    }
    return permissions;
  }

  private Permission readWildcardPermission(Record record) {
    Permission wp = new Permission();
    wp.setTable("*");
    String selectLevel = record.get(RP_SELECT_LEVEL);
    if (selectLevel != null) {
      wp.setSelect(SelectLevel.valueOf(selectLevel));
    }
    Boolean insertRls = record.get(RP_INSERT_RLS);
    if (Boolean.TRUE.equals(insertRls)) wp.setInsert(ModifyLevel.ROW);
    else if (insertRls != null) wp.setInsert(ModifyLevel.TABLE);
    Boolean updateRls = record.get(RP_UPDATE_RLS);
    if (Boolean.TRUE.equals(updateRls)) wp.setUpdate(ModifyLevel.ROW);
    else if (updateRls != null) wp.setUpdate(ModifyLevel.TABLE);
    Boolean deleteRls = record.get(RP_DELETE_RLS);
    if (Boolean.TRUE.equals(deleteRls)) wp.setDelete(ModifyLevel.ROW);
    else if (deleteRls != null) wp.setDelete(ModifyLevel.TABLE);
    Boolean grantPerm = record.get(RP_GRANT_PERMISSION);
    if (Boolean.TRUE.equals(grantPerm)) wp.setGrant(true);
    wp.setColumnAccess(readColumnAccess(record));
    return wp;
  }

  private ColumnAccess readColumnAccess(Record record) {
    String[] editableCols = record.get(RP_EDITABLE_COLUMNS);
    String[] readonlyCols = record.get(RP_READONLY_COLUMNS);
    String[] hiddenCols = record.get(RP_HIDDEN_COLUMNS);
    if (editableCols == null && readonlyCols == null && hiddenCols == null) {
      return null;
    }
    return new ColumnAccess(
        editableCols != null ? List.of(editableCols) : null,
        readonlyCols != null ? List.of(readonlyCols) : null,
        hiddenCols != null ? List.of(hiddenCols) : null);
  }

  private SelectLevel mapSelectLevel(Boolean canSelect, String selectLevel) {
    if (Boolean.TRUE.equals(canSelect)) {
      if ("ROW".equals(selectLevel)) return SelectLevel.ROW;
      return SelectLevel.TABLE;
    }
    if (selectLevel != null) {
      return SelectLevel.valueOf(selectLevel);
    }
    return null;
  }

  private ModifyLevel mapModifyLevel(Boolean canModify, Boolean isRls) {
    if (Boolean.TRUE.equals(canModify)) {
      return Boolean.TRUE.equals(isRls) ? ModifyLevel.ROW : ModifyLevel.TABLE;
    }
    return null;
  }

  private void revokeAllMemberships(String fullRole) {
    jooq()
        .execute(
            """
            DO $$
            DECLARE
              member TEXT;
            BEGIN
              FOR member IN SELECT rolname FROM pg_roles
                WHERE pg_has_role(rolname, {0}, 'member') AND rolname <> {0}
              LOOP
                EXECUTE 'REVOKE ' || quote_ident({0}) || ' FROM ' || quote_ident(member);
              END LOOP;
            END
            $$;\
            """,
            inline(fullRole));
  }

  private static Boolean toRlsFlag(ModifyLevel level) {
    if (level == ModifyLevel.ROW) return Boolean.TRUE;
    if (level == ModifyLevel.TABLE) return Boolean.FALSE;
    return null;
  }

  private String loadResource(String resourceName) {
    try (InputStream is =
        getClass().getResourceAsStream("/org/molgenis/emx2/sql/" + resourceName)) {
      if (is == null) {
        throw new MolgenisException("Resource not found: " + resourceName);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new MolgenisException("Failed to load resource: " + resourceName, e);
    }
  }
}
