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
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.ColumnAccess;
import org.molgenis.emx2.ModifyLevel;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Permission;
import org.molgenis.emx2.RoleInfo;
import org.molgenis.emx2.SelectLevel;

public class SqlRoleManager {
  static final Set<String> SYSTEM_ROLE_NAMES =
      Set.of(
          ROLE_EXISTS,
          ROLE_RANGE,
          ROLE_AGGREGATOR,
          ROLE_COUNT,
          ROLE_VIEWER,
          ROLE_EDITOR,
          ROLE_MANAGER,
          ROLE_OWNER,
          ROLE_ADMIN);

  private static final org.jooq.Table<?> RLS_PERMISSIONS_TABLE =
      table(name("MOLGENIS", "rls_permissions"));
  private static final org.jooq.Field<String> RP_ROLE = field(name("role_name"), VARCHAR);
  private static final org.jooq.Field<String> RP_SCHEMA = field(name("table_schema"), VARCHAR);
  private static final org.jooq.Field<String> RP_TABLE = field(name("table_name"), VARCHAR);
  private static final org.jooq.Field<String> RP_SELECT_LEVEL =
      field(name("select_level"), VARCHAR);
  private static final org.jooq.Field<Boolean> RP_INSERT_RLS = field(name("insert_rls"), BOOLEAN);
  private static final org.jooq.Field<Boolean> RP_UPDATE_RLS = field(name("update_rls"), BOOLEAN);
  private static final org.jooq.Field<Boolean> RP_DELETE_RLS = field(name("delete_rls"), BOOLEAN);
  private static final org.jooq.Field<Boolean> RP_GRANT_PERMISSION =
      field(name("grant_permission"), BOOLEAN);
  private static final org.jooq.Field<String[]> RP_EDITABLE_COLUMNS =
      field(name("editable_columns"), VARCHAR.getArrayDataType());
  private static final org.jooq.Field<String[]> RP_READONLY_COLUMNS =
      field(name("readonly_columns"), VARCHAR.getArrayDataType());
  private static final org.jooq.Field<String[]> RP_HIDDEN_COLUMNS =
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
      org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
      jooq().execute("REVOKE ALL ON {0} FROM {1}", jooqTable, name(fullRole));

      Integer hasColumn =
          jooq()
              .selectCount()
              .from("information_schema.columns")
              .where(field("table_schema").eq(inline(schemaName)))
              .and(field("table_name").eq(inline(tableName)))
              .and(field("column_name").eq(inline(MG_ROLES)))
              .fetchOne(0, Integer.class);
      if (hasColumn != null && hasColumn > 0) {
        jooq()
            .execute(
                "UPDATE {0} SET {1} = array_remove({1}, {2}) WHERE {1} @> ARRAY[{2}]",
                jooqTable, name(MG_ROLES), inline(fullRole));
      }
    }

    jooq()
        .deleteFrom(RLS_PERMISSIONS_TABLE)
        .where(RP_SCHEMA.eq(schemaName))
        .and(RP_ROLE.eq(fullRole))
        .execute();

    revokeAllMemberships(fullRole);
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
  }

  public void revoke(String schemaName, String roleName, Permission permission) {
    String fullRole = fullRoleName(schemaName, roleName);
    String tableName = permission.getTable();

    if (tableName == null) {
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
  }

  private void revokeTableGrants(
      String schemaName, String fullRole, String tableName, Permission permission) {
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
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
        jooq()
            .deleteFrom(RLS_PERMISSIONS_TABLE)
            .where(RP_SCHEMA.eq(schemaName))
            .and(RP_ROLE.eq(fullRole))
            .and(RP_TABLE.eq(tableName))
            .execute();
      } else {
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
    }
  }

  private void syncPermissionGrants(
      String schemaName, String fullRole, String tableName, Permission permission) {
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
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
      editableCols = ca.getEditable() != null ? ca.getEditable().toArray(new String[0]) : null;
      readonlyCols = ca.getReadonly() != null ? ca.getReadonly().toArray(new String[0]) : null;
      hiddenCols = ca.getHidden() != null ? ca.getHidden().toArray(new String[0]) : null;
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

  public void enableRowLevelSecurity(String schemaName, String tableName) {
    DSLContext ctx = jooq();
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));

    Integer hasColumn =
        ctx.selectCount()
            .from("information_schema.columns")
            .where(field("table_schema").eq(inline(schemaName)))
            .and(field("table_name").eq(inline(tableName)))
            .and(field("column_name").eq(inline(MG_ROLES)))
            .fetchOne(0, Integer.class);

    if (hasColumn != null && hasColumn > 0) {
      return;
    }

    ctx.execute("ALTER TABLE {0} ADD COLUMN {1} TEXT[] DEFAULT NULL", jooqTable, name(MG_ROLES));
    ctx.execute(
        "CREATE INDEX IF NOT EXISTS {0} ON {1} USING GIN({2})",
        name(tableName + "_" + MG_ROLES + "_idx"), jooqTable, name(MG_ROLES));
    ctx.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", jooqTable);

    String fqTable = schemaName + "." + tableName;

    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_select"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_insert"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_update"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_delete"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_modify"), jooqTable);

    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR SELECT USING ("
            + "{2} != ALL(string_to_array(COALESCE(current_setting('molgenis.rls_select_tables', true), ''), ',')) "
            + "OR {3} IS NULL "
            + "OR {3} && string_to_array(COALESCE(current_setting('molgenis.active_role', true), ''), ',')"
            + ")",
        name(tableName + "_rls_select"), jooqTable, inline(fqTable), name(MG_ROLES));

    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR INSERT WITH CHECK ("
            + "{2} != ALL(string_to_array(COALESCE(current_setting('molgenis.rls_insert_tables', true), ''), ',')) "
            + "OR {3} IS NULL "
            + "OR {3} && string_to_array(COALESCE(current_setting('molgenis.active_role', true), ''), ',')"
            + ")",
        name(tableName + "_rls_insert"), jooqTable, inline(fqTable), name(MG_ROLES));

    String updateExpr =
        "{2} != ALL(string_to_array(COALESCE(current_setting('molgenis.rls_update_tables', true), ''), ',')) "
            + "OR {3} IS NULL "
            + "OR {3} && string_to_array(COALESCE(current_setting('molgenis.active_role', true), ''), ',')";
    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR UPDATE USING ("
            + updateExpr
            + ") WITH CHECK ("
            + updateExpr
            + ")",
        name(tableName + "_rls_update"),
        jooqTable,
        inline(fqTable),
        name(MG_ROLES));

    ctx.execute(
        "CREATE POLICY {0} ON {1} FOR DELETE USING ("
            + "{2} != ALL(string_to_array(COALESCE(current_setting('molgenis.rls_delete_tables', true), ''), ',')) "
            + "OR {3} IS NULL "
            + "OR {3} && string_to_array(COALESCE(current_setting('molgenis.active_role', true), ''), ',')"
            + ")",
        name(tableName + "_rls_delete"), jooqTable, inline(fqTable), name(MG_ROLES));
  }

  public void disableRowLevelSecurity(String schemaName, String tableName) {
    DSLContext ctx = jooq();
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_select"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_insert"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_update"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_delete"), jooqTable);
    ctx.execute("DROP POLICY IF EXISTS {0} ON {1}", name(tableName + "_rls_modify"), jooqTable);
    ctx.execute("ALTER TABLE {0} DISABLE ROW LEVEL SECURITY", jooqTable);
  }

  public RoleInfo getRoleInfo(String schemaName, String roleName) {
    RoleInfo info = new RoleInfo(roleName);
    info.setSystem(isSystemRole(roleName));
    info.setDescription(getDescription(schemaName, roleName));
    info.setPermissions(getPermissions(schemaName, roleName));
    return info;
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

        String[] editableCols = record.get("editable_columns", String[].class);
        String[] readonlyCols = record.get("readonly_columns", String[].class);
        String[] hiddenCols = record.get("hidden_columns", String[].class);
        if (editableCols != null || readonlyCols != null || hiddenCols != null) {
          ColumnAccess ca = new ColumnAccess();
          if (editableCols != null) ca.setEditable(List.of(editableCols));
          if (readonlyCols != null) ca.setReadonly(List.of(readonlyCols));
          if (hiddenCols != null) ca.setHidden(List.of(hiddenCols));
          p.setColumnAccess(ca);
        }

        permissions.add(p);
      }

      Record wildcardRecord =
          jooq()
              .selectFrom(RLS_PERMISSIONS_TABLE)
              .where(RP_ROLE.eq(fullRole))
              .and(RP_SCHEMA.eq(schemaName))
              .and(RP_TABLE.eq("*"))
              .fetchOne();
      if (wildcardRecord != null) {
        Permission wp = new Permission();
        wp.setTable("*");
        String selectLevel = wildcardRecord.get(RP_SELECT_LEVEL);
        if (selectLevel != null) {
          wp.setSelect(SelectLevel.valueOf(selectLevel));
        }
        Boolean insertRls = wildcardRecord.get(RP_INSERT_RLS);
        if (Boolean.TRUE.equals(insertRls)) wp.setInsert(ModifyLevel.ROW);
        else if (insertRls != null) wp.setInsert(ModifyLevel.TABLE);
        Boolean updateRls = wildcardRecord.get(RP_UPDATE_RLS);
        if (Boolean.TRUE.equals(updateRls)) wp.setUpdate(ModifyLevel.ROW);
        else if (updateRls != null) wp.setUpdate(ModifyLevel.TABLE);
        Boolean deleteRls = wildcardRecord.get(RP_DELETE_RLS);
        if (Boolean.TRUE.equals(deleteRls)) wp.setDelete(ModifyLevel.ROW);
        else if (deleteRls != null) wp.setDelete(ModifyLevel.TABLE);
        Boolean grantPerm = wildcardRecord.get(RP_GRANT_PERMISSION);
        if (Boolean.TRUE.equals(grantPerm)) wp.setGrant(true);

        String[] editableCols = wildcardRecord.get(RP_EDITABLE_COLUMNS);
        String[] readonlyCols = wildcardRecord.get(RP_READONLY_COLUMNS);
        String[] hiddenCols = wildcardRecord.get(RP_HIDDEN_COLUMNS);
        if (editableCols != null || readonlyCols != null || hiddenCols != null) {
          ColumnAccess ca = new ColumnAccess();
          if (editableCols != null) ca.setEditable(List.of(editableCols));
          if (readonlyCols != null) ca.setReadonly(List.of(readonlyCols));
          if (hiddenCols != null) ca.setHidden(List.of(hiddenCols));
          wp.setColumnAccess(ca);
        }
        permissions.add(wp);
      }
    } catch (Exception e) {
    }

    return permissions;
  }

  public List<Permission> getMyPermissions(String schemaName) {
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
    if (SYSTEM_ROLE_NAMES.contains(roleName)) {
      return getSystemRolePermissions(schema, roleName);
    }
    return getPermissions(schemaName, roleName);
  }

  private List<Permission> getSystemRolePermissions(SqlSchema schema, String roleName) {
    List<Permission> permissions = new ArrayList<>();
    for (String tableName : schema.getTableNames()) {
      Permission perm = new Permission(tableName);
      switch (roleName) {
        case ROLE_OWNER:
        case ROLE_MANAGER:
        case ROLE_EDITOR:
          perm.setSelect(SelectLevel.TABLE);
          perm.setInsert(ModifyLevel.TABLE);
          perm.setUpdate(ModifyLevel.TABLE);
          perm.setDelete(ModifyLevel.TABLE);
          break;
        case ROLE_VIEWER:
          perm.setSelect(SelectLevel.TABLE);
          break;
        case ROLE_COUNT:
          perm.setSelect(SelectLevel.COUNT);
          break;
        case ROLE_AGGREGATOR:
          perm.setSelect(SelectLevel.AGGREGATOR);
          break;
        case ROLE_RANGE:
          perm.setSelect(SelectLevel.RANGE);
          break;
        case ROLE_EXISTS:
          perm.setSelect(SelectLevel.EXISTS);
          break;
        default:
          break;
      }
      permissions.add(perm);
    }
    return permissions;
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

  public void cleanupTablePermissions(String schemaName, String tableName) {
    try {
      jooq()
          .deleteFrom(RLS_PERMISSIONS_TABLE)
          .where(RP_SCHEMA.eq(schemaName))
          .and(RP_TABLE.eq(tableName))
          .execute();
    } catch (Exception e) {
    }
  }

  public void cleanupSchemaPermissions(String schemaName) {
    try {
      jooq().deleteFrom(RLS_PERMISSIONS_TABLE).where(RP_SCHEMA.eq(schemaName)).execute();
    } catch (Exception e) {
    }
  }

  public boolean isSystemRole(String roleName) {
    return SYSTEM_ROLE_NAMES.contains(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  public static boolean isGlobalRole(String fullRoleName) {
    return fullRoleName != null && fullRoleName.startsWith(MG_ROLE_PREFIX + "*/");
  }

  private void revokeAllMemberships(String fullRole) {
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
