package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.SQLDataType.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.RoleInfo;

public class SqlRoleManager {
  static final List<String> SYSTEM_ROLES =
      List.of(
          ROLE_EXISTS,
          ROLE_RANGE,
          ROLE_AGGREGATOR,
          ROLE_COUNT,
          ROLE_VIEWER,
          ROLE_EDITOR,
          ROLE_MANAGER,
          ROLE_OWNER);

  private static final String MG_ROWLEVEL = "MG_ROWLEVEL";
  private static final org.jooq.Table PERMISSION_METADATA =
      table(name("MOLGENIS", "permission_metadata"));
  private static final org.jooq.Field<String> PM_SCHEMA = field(name("table_schema"), VARCHAR);
  private static final org.jooq.Field<String> PM_ROLE = field(name("role_name"), VARCHAR);
  private static final org.jooq.Field<String> PM_TABLE = field(name("table_name"), VARCHAR);
  private static final org.jooq.Field<String[]> PM_EDIT_COLUMNS =
      field(name("edit_columns"), VARCHAR.getArrayDataType());
  private static final org.jooq.Field<String[]> PM_DENY_COLUMNS =
      field(name("deny_columns"), VARCHAR.getArrayDataType());

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
    String existsRole = fullRoleName(schemaName, ROLE_EXISTS);

    executeCreateRole(jooq(), fullRole);
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

    for (String tableName : database.getSchema(schemaName).getTableNames()) {
      org.jooq.Table jooqTable = table(name(schemaName, tableName));
      jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
      jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
      jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
      jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
    }

    jooq()
        .deleteFrom(PERMISSION_METADATA)
        .where(PM_SCHEMA.eq(schemaName))
        .and(PM_ROLE.eq(roleName))
        .execute();

    jooq()
        .execute(
            "DO $$\n"
                + "DECLARE\n"
                + "  member TEXT;\n"
                + "BEGIN\n"
                + "  FOR member IN SELECT rolname FROM pg_roles WHERE pg_has_role(rolname, {0}, 'member') AND rolname <> {0}\n"
                + "  LOOP\n"
                + "    EXECUTE 'REVOKE ' || quote_ident({0}) || ' FROM ' || quote_ident(member);\n"
                + "  END LOOP;\n"
                + "END\n"
                + "$$;",
            inline(fullRole));

    jooq().execute("DROP ROLE IF EXISTS {0}", name(fullRole));
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

  public void addMember(String schemaName, String roleName, String userName) {
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }

    if (!database.hasUser(userName)) {
      database.addUser(userName);
    }

    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + userName;
    jooq().execute("GRANT {0} TO {1}", name(fullRole), name(fullUser));
  }

  public void removeMember(String schemaName, String roleName, String userName) {
    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + userName;
    jooq().execute("REVOKE {0} FROM {1}", name(fullRole), name(fullUser));
  }

  public void setPermission(String schemaName, String roleName, Permission permission) {
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }

    String tableName = permission.getTable();
    if (tableName != null && !database.getSchema(schemaName).getTableNames().contains(tableName)) {
      throw new MolgenisException("Table does not exist: " + tableName);
    }

    if (permission.isRevocation()) {
      revokePermission(schemaName, roleName, tableName, permission.isRowLevel());
      return;
    }

    syncPermissionGrants(schemaName, roleName, permission);
    syncColumnRestrictions(schemaName, roleName, permission);

    if (permission.isRowLevel()) {
      String fullRole = fullRoleName(schemaName, roleName);
      jooq().execute("GRANT {0} TO {1}", name(MG_ROWLEVEL), name(fullRole));

      if (tableName != null) {
        enableRowLevelSecurity(schemaName, tableName);
      }
    }
  }

  private void syncColumnRestrictions(String schemaName, String roleName, Permission permission) {
    String tableName = permission.getTable();
    if (tableName == null) {
      return;
    }

    if (permission.getEditColumns() == null && permission.getDenyColumns() == null) {
      jooq()
          .deleteFrom(PERMISSION_METADATA)
          .where(PM_SCHEMA.eq(schemaName))
          .and(PM_ROLE.eq(roleName))
          .and(PM_TABLE.eq(tableName))
          .execute();
      return;
    }

    jooq()
        .insertInto(PERMISSION_METADATA)
        .set(PM_SCHEMA, schemaName)
        .set(PM_ROLE, roleName)
        .set(PM_TABLE, tableName)
        .set(
            PM_EDIT_COLUMNS,
            permission.getEditColumns() != null
                ? permission.getEditColumns().toArray(new String[0])
                : null)
        .set(
            PM_DENY_COLUMNS,
            permission.getDenyColumns() != null
                ? permission.getDenyColumns().toArray(new String[0])
                : null)
        .onConflict(PM_SCHEMA, PM_ROLE, PM_TABLE)
        .doUpdate()
        .set(
            PM_EDIT_COLUMNS,
            permission.getEditColumns() != null
                ? permission.getEditColumns().toArray(new String[0])
                : null)
        .set(
            PM_DENY_COLUMNS,
            permission.getDenyColumns() != null
                ? permission.getDenyColumns().toArray(new String[0])
                : null)
        .execute();
  }

  public void revokePermission(
      String schemaName, String roleName, String tableName, boolean rowLevel) {
    String fullRole = fullRoleName(schemaName, roleName);

    if (tableName != null) {
      org.jooq.Table jooqTable = table(name(schemaName, tableName));
      jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
      jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
      jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
      jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));

      jooq()
          .deleteFrom(PERMISSION_METADATA)
          .where(PM_SCHEMA.eq(schemaName))
          .and(PM_ROLE.eq(roleName))
          .and(PM_TABLE.eq(tableName))
          .execute();
    } else {
      for (String table : database.getSchema(schemaName).getTableNames()) {
        org.jooq.Table jooqTable = table(name(schemaName, table));
        jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
        jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
        jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
        jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
      }

      jooq()
          .deleteFrom(PERMISSION_METADATA)
          .where(PM_SCHEMA.eq(schemaName))
          .and(PM_ROLE.eq(roleName))
          .execute();
    }

    if (rowLevel) {
      jooq().execute("REVOKE {0} FROM {1}", name(MG_ROWLEVEL), name(fullRole));
    }
  }

  public List<Permission> getPermissions(String schemaName, String roleName) {
    String fullRole = fullRoleName(schemaName, roleName);
    List<Permission> permissions = new ArrayList<>();

    for (String tableName : database.getSchema(schemaName).getTableNames()) {
      String qualifiedTable = schemaName + "." + tableName;

      Boolean hasSelect = hasTablePrivilege(fullRole, qualifiedTable, "SELECT");
      Boolean hasInsert = hasTablePrivilege(fullRole, qualifiedTable, "INSERT");
      Boolean hasUpdate = hasTablePrivilege(fullRole, qualifiedTable, "UPDATE");
      Boolean hasDelete = hasTablePrivilege(fullRole, qualifiedTable, "DELETE");

      if (Boolean.TRUE.equals(hasSelect)
          || Boolean.TRUE.equals(hasInsert)
          || Boolean.TRUE.equals(hasUpdate)
          || Boolean.TRUE.equals(hasDelete)) {
        Permission p = new Permission();
        p.setTable(tableName);
        p.setRowLevel(isRowLevelRole(fullRole));
        p.setSelect(hasSelect);
        p.setInsert(hasInsert);
        p.setUpdate(hasUpdate);
        p.setDelete(hasDelete);

        mergeColumnRestrictions(schemaName, roleName, tableName, p);
        permissions.add(p);
      }
    }

    return permissions;
  }

  public List<Permission> getAllPermissions(String schemaName) {
    List<Permission> permissions = new ArrayList<>();
    for (String roleName : getRolesForSchema(schemaName)) {
      permissions.addAll(getPermissions(schemaName, roleName));
    }
    return permissions;
  }

  private Boolean hasTablePrivilege(String role, String qualifiedTable, String privilege) {
    try {
      String quoted = "\"" + qualifiedTable.replace(".", "\".\"") + "\"";
      return jooq()
          .fetchOne("SELECT has_table_privilege(?, ?::regclass, ?)", role, quoted, privilege)
          .get(0, Boolean.class);
    } catch (Exception e) {
      return false;
    }
  }

  private boolean isRowLevelRole(String fullRole) {
    try {
      Boolean result =
          jooq()
              .fetchOne("SELECT pg_has_role(?, ?, 'member')", fullRole, MG_ROWLEVEL)
              .get(0, Boolean.class);
      return Boolean.TRUE.equals(result);
    } catch (Exception e) {
      return false;
    }
  }

  private void mergeColumnRestrictions(
      String schemaName, String roleName, String tableName, Permission permission) {
    Record record =
        jooq()
            .selectFrom(PERMISSION_METADATA)
            .where(PM_SCHEMA.eq(schemaName))
            .and(PM_ROLE.eq(roleName))
            .and(PM_TABLE.eq(tableName))
            .fetchOne();

    if (record != null) {
      String[] editCols = record.get(PM_EDIT_COLUMNS);
      if (editCols != null) {
        permission.setEditColumns(List.of(editCols));
      }
      String[] denyCols = record.get(PM_DENY_COLUMNS);
      if (denyCols != null) {
        permission.setDenyColumns(List.of(denyCols));
      }
    }
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
      String shortRole = fullRole.substring(rolePrefix.length());
      roles.add(shortRole);
    }
    return roles;
  }

  public List<RoleInfo> getRoleInfos(String schemaName) {
    List<RoleInfo> roleInfos = new ArrayList<>();
    for (String roleName : getRolesForSchema(schemaName)) {
      RoleInfo info = new RoleInfo(roleName);
      info.setSystem(isSystemRole(roleName));
      info.setDescription(getDescription(schemaName, roleName));
      info.setPermissions(getPermissions(schemaName, roleName));
      roleInfos.add(info);
    }
    return roleInfos;
  }

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

  public void enableRowLevelSecurity(String schemaName, String tableName) {
    DSLContext jooq = jooq();
    org.jooq.Table jooqTable = table(name(schemaName, tableName));

    Integer hasColumn =
        jooq.selectCount()
            .from("information_schema.columns")
            .where(field("table_schema").eq(inline(schemaName)))
            .and(field("table_name").eq(inline(tableName)))
            .and(field("column_name").eq(inline(MG_ROLES)))
            .fetchOne(0, Integer.class);

    if (hasColumn != null && hasColumn > 0) {
      return;
    }

    jooq.execute("ALTER TABLE {0} ADD COLUMN {1} TEXT[] DEFAULT NULL", jooqTable, name(MG_ROLES));

    jooq.execute(
        "CREATE INDEX IF NOT EXISTS {0} ON {1} USING GIN({2})",
        name(tableName + "_" + MG_ROLES + "_idx"), jooqTable, name(MG_ROLES));

    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", jooqTable);

    String policySelect = tableName + "_rls_select";
    String policyModify = tableName + "_rls_modify";

    jooq.execute("DROP POLICY IF EXISTS {0} ON {1}", name(policySelect), jooqTable);
    jooq.execute("DROP POLICY IF EXISTS {0} ON {1}", name(policyModify + "_insert"), jooqTable);
    jooq.execute("DROP POLICY IF EXISTS {0} ON {1}", name(policyModify + "_update"), jooqTable);
    jooq.execute("DROP POLICY IF EXISTS {0} ON {1}", name(policyModify + "_delete"), jooqTable);

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT\n"
            + "  USING (\n"
            + "    {2} IS NULL\n"
            + "    OR pg_has_role(current_setting('molgenis.user', true)::text, {3}, 'member')\n"
            + "    OR {2} && string_to_array(current_setting('molgenis.roles', true), ',')\n"
            + "  )",
        name(policySelect), jooqTable, name(MG_ROLES), inline(MG_ROWLEVEL));

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR INSERT\n"
            + "  WITH CHECK (\n"
            + "    {2} IS NULL\n"
            + "    OR pg_has_role(current_setting('molgenis.user', true)::text, {3}, 'member')\n"
            + "    OR {2} && string_to_array(current_setting('molgenis.roles', true), ',')\n"
            + "  )",
        name(policyModify + "_insert"), jooqTable, name(MG_ROLES), inline(MG_ROWLEVEL));

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR UPDATE\n"
            + "  USING (\n"
            + "    {2} IS NULL\n"
            + "    OR pg_has_role(current_setting('molgenis.user', true)::text, {3}, 'member')\n"
            + "    OR {2} && string_to_array(current_setting('molgenis.roles', true), ',')\n"
            + "  )",
        name(policyModify + "_update"), jooqTable, name(MG_ROLES), inline(MG_ROWLEVEL));

    jooq.execute(
        "CREATE POLICY {0} ON {1} FOR DELETE\n"
            + "  USING (\n"
            + "    {2} IS NULL\n"
            + "    OR pg_has_role(current_setting('molgenis.user', true)::text, {3}, 'member')\n"
            + "    OR {2} && string_to_array(current_setting('molgenis.roles', true), ',')\n"
            + "  )",
        name(policyModify + "_delete"), jooqTable, name(MG_ROLES), inline(MG_ROWLEVEL));
  }

  private void syncPermissionGrants(String schemaName, String roleName, Permission permission) {
    String fullRole = fullRoleName(schemaName, roleName);
    List<String> tables = new ArrayList<>();

    if (permission.getTable() != null) {
      tables.add(permission.getTable());
    } else {
      tables.addAll(database.getSchema(schemaName).getTableNames());
    }

    for (String tableName : tables) {
      org.jooq.Table jooqTable = table(name(schemaName, tableName));

      if (Boolean.TRUE.equals(permission.getSelect())) {
        jooq().execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
      } else if (Boolean.FALSE.equals(permission.getSelect())) {
        jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
      }

      if (Boolean.TRUE.equals(permission.getInsert())) {
        jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
      } else if (Boolean.FALSE.equals(permission.getInsert())) {
        jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
      }

      if (Boolean.TRUE.equals(permission.getUpdate())) {
        jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
      } else if (Boolean.FALSE.equals(permission.getUpdate())) {
        jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
      }

      if (Boolean.TRUE.equals(permission.getDelete())) {
        jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
      } else if (Boolean.FALSE.equals(permission.getDelete())) {
        jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
      }
    }
  }

  public boolean isSystemRole(String roleName) {
    return SYSTEM_ROLES.contains(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }
}
