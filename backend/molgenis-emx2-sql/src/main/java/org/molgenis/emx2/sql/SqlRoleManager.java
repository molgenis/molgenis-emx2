package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING_ARRAY;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.*;

public class SqlRoleManager {

  public static final String PG_ROLES = "pg_roles";
  public static final String ROLNAME = "rolname";
  public static final int PG_MAX_ID_LENGTH = 63;
  public static final String RLS_ROLE_PREFIX = "RLS_";

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
    if (fullRole.getBytes(UTF_8).length > PG_MAX_ID_LENGTH) {
      throw new MolgenisException(
          "Role name '"
              + roleName
              + "' is too long: the combined identifier '"
              + fullRole
              + "' exceeds PostgreSQL's 63-byte limit");
    }
    String existsRole = fullRoleName(schemaName, Privileges.EXISTS.toString());
    String ownerRole = fullRoleName(schemaName, Privileges.OWNER.toString());
    database.tx( // we need to lift to admin to create a role
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            executeCreateRole(jooq, fullRole);
            jooq.execute("GRANT {0} TO session_user WITH ADMIN OPTION", name(fullRole));
            jooq.execute("GRANT {0} TO {1} WITH ADMIN OPTION", name(fullRole), name(ownerRole));
            jooq.execute("GRANT {0} TO {1}", name(existsRole), name(fullRole));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  public void deleteRole(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot delete system role: " + roleName);
    }
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    String fullRole = fullRoleName(schemaName, roleName);
    String rlsFullRole = fullRoleName(schemaName, RLS_ROLE_PREFIX + roleName);
    database.tx( // we need to lift to admin to drop a role
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            Collection<String> tableNames = database.getSchema(schemaName).getTableNames();
            dropPgRoleAndCleanup(jooq, schemaName, fullRole, tableNames);
            // Also clean up the companion RLS role if it exists
            if (jooq.fetchExists(
                jooq.select().from(PG_ROLES).where(field(ROLNAME).eq(inline(rlsFullRole))))) {
              dropPgRoleAndCleanup(jooq, schemaName, rlsFullRole, tableNames);
            }
          } finally {
            db.setActiveUser(currentUser);
          }
        });
    database.getListener().onSchemaChange();
  }

  private static void dropPgRoleAndCleanup(
      DSLContext jooq, String schemaName, String fullRole, Collection<String> tableNames) {
    for (String tableName : tableNames) {
      jooq.execute(
          "REVOKE ALL ON {0} FROM {1}", table(name(schemaName, tableName)), name(fullRole));
    }
    jooq.execute(
        """
            DO $$ DECLARE m TEXT; BEGIN
             FOR m IN SELECT rolname FROM pg_roles
             WHERE pg_has_role(rolname, {0}, 'member') AND rolname <> {0}
             LOOP EXECUTE 'REVOKE ' || quote_ident({0}) || ' FROM ' || quote_ident(m);
             END LOOP; END $$;""",
        inline(fullRole));
    jooq.execute("DROP ROLE IF EXISTS {0}", name(fullRole));
  }

  public boolean roleExists(String schemaName, String roleName) {
    return jooq()
        .fetchExists(
            jooq()
                .select()
                .from(PG_ROLES)
                .where(field(ROLNAME).eq(inline(fullRoleName(schemaName, roleName)))));
  }

  public void grant(String schemaName, String roleName, TablePermission permission) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot grant custom permissions to system role: " + roleName);
    }
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    String tableName = permission.table();
    if (tableName == null) {
      throw new MolgenisException("Table name is required for table-level grant");
    }
    if (!database.getSchema(schemaName).getTableNames().contains(tableName)) {
      throw new MolgenisException("Table does not exist: " + tableName);
    }
    String grantRoleName;
    if (Boolean.TRUE.equals(permission.isRowLevel())) {
      grantRoleName = RLS_ROLE_PREFIX + roleName;
      createRlsRole(schemaName, roleName);
    } else {
      grantRoleName = roleName;
    }
    String fullRole = fullRoleName(schemaName, grantRoleName);
    applyPgGrants(schemaName, fullRole, tableName, permission);
    if (Boolean.TRUE.equals(permission.isRowLevel())) {
      enableRowLevelSecurity(schemaName, tableName);
    }
    database.getListener().onSchemaChange();
  }

  private void createRlsRole(String schemaName, String roleName) {
    String rlsRoleName = RLS_ROLE_PREFIX + roleName;
    if (!roleExists(schemaName, rlsRoleName)) {
      createRole(schemaName, rlsRoleName);
    }
    String rlsFullRole = fullRoleName(schemaName, rlsRoleName);
    String regularFullRole = fullRoleName(schemaName, roleName);
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            ((SqlDatabase) db)
                .getJooq()
                .execute("GRANT {0} TO {1}", name(rlsFullRole), name(regularFullRole));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  private void enableRowLevelSecurity(String schemaName, String tableName) {
    TableMetadata tableMetadata =
        database.getSchema(schemaName).getMetadata().getTableMetadata(tableName);
    if (tableMetadata.getColumn(MG_ROLES) == null) {
      tableMetadata.add(column(MG_ROLES).setType(STRING_ARRAY));
    }
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    jooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", jooqTable);

    String viewerRole = fullRoleName(schemaName, Privileges.VIEWER.toString());
    String editorRole = fullRoleName(schemaName, Privileges.EDITOR.toString());
    String selectClause = rlsAccessClause(schemaName, tableName, viewerRole);
    String dmlClause = rlsAccessClause(schemaName, tableName, editorRole);

    jooq().execute("DROP POLICY IF EXISTS mg_roles_select_policy ON {0}", jooqTable);
    jooq()
        .execute(
            "CREATE POLICY mg_roles_select_policy ON {0} FOR SELECT USING (" + selectClause + ")",
            jooqTable);

    jooq().execute("DROP POLICY IF EXISTS mg_roles_dml_policy ON {0}", jooqTable);
    jooq()
        .execute(
            "CREATE POLICY mg_roles_dml_policy ON {0} FOR ALL"
                + " USING ("
                + dmlClause
                + ") WITH CHECK ("
                + dmlClause
                + ")",
            jooqTable);
  }

  private String rlsAccessClause(String schemaName, String tableName, String bypassRole) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    String rlsRolePrefix = rolePrefix + RLS_ROLE_PREFIX;
    String systemRoles =
        Arrays.stream(Privileges.values())
            .map(p -> jooq().render(inline(fullRoleName(schemaName, p.toString()))))
            .collect(Collectors.joining(", "));

    return """
        pg_has_role(current_user, %s, 'member')
        OR EXISTS (
          SELECT 1 FROM pg_roles r
          CROSS JOIN LATERAL (
            SELECT c.relacl FROM pg_class c
            JOIN pg_namespace n ON n.oid = c.relnamespace
            WHERE n.nspname = %s AND c.relname = %s
          ) tbl
          WHERE r.rolname LIKE %s
            AND r.rolname NOT LIKE %s
            AND r.rolname NOT IN (%s)
            AND pg_has_role(current_user, r.rolname, 'member')
            AND tbl.relacl IS NOT NULL
            AND EXISTS (SELECT 1 FROM aclexplode(tbl.relacl) ace WHERE ace.grantee = r.oid)
        )
        OR EXISTS (
          SELECT 1 FROM unnest(mg_roles) r
          WHERE pg_has_role(current_user, %s || r, 'member')
        )"""
        .formatted(
            jooq().render(inline(bypassRole)),
            jooq().render(inline(schemaName)),
            jooq().render(inline(tableName)),
            jooq().render(inline(rolePrefix + "%")),
            jooq().render(inline(rlsRolePrefix + "%")),
            systemRoles,
            jooq().render(inline(rolePrefix)));
  }

  public void revoke(String schemaName, String roleName, String tableName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot revoke permissions from system role: " + roleName);
    }
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    String fullRole = fullRoleName(schemaName, roleName);
    jooq()
        .execute("REVOKE ALL ON {0} FROM {1}", table(name(schemaName, tableName)), name(fullRole));
    // Also revoke from the RLS-prefixed role if it exists
    String rlsFullRole = fullRoleName(schemaName, RLS_ROLE_PREFIX + roleName);
    if (jooq()
        .fetchExists(
            jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(rlsFullRole))))) {
      jooq()
          .execute(
              "REVOKE ALL ON {0} FROM {1}", table(name(schemaName, tableName)), name(rlsFullRole));
    }
    disableRowLevelSecurityIfUnused(schemaName, tableName);
    database.getListener().onSchemaChange();
  }

  private void disableRowLevelSecurityIfUnused(String schemaName, String tableName) {
    String rlsRolePrefix = MG_ROLE_PREFIX + schemaName + "/" + RLS_ROLE_PREFIX;
    boolean anyRlsGrantRemains =
        jooq()
            .fetchExists(
                jooq()
                    .select()
                    .from("information_schema.role_table_grants")
                    .where(field("table_schema").eq(inline(schemaName)))
                    .and(field("table_name").eq(inline(tableName)))
                    .and(field("grantee").like(inline(rlsRolePrefix + "%"))));
    if (!anyRlsGrantRemains) {
      org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
      jooq().execute("DROP POLICY IF EXISTS mg_roles_select_policy ON {0}", jooqTable);
      jooq().execute("DROP POLICY IF EXISTS mg_roles_dml_policy ON {0}", jooqTable);
      jooq().execute("ALTER TABLE {0} DISABLE ROW LEVEL SECURITY", jooqTable);
    }
  }

  private void applyPgGrants(
      String schemaName, String fullRole, String tableName, TablePermission p) {
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    if (Boolean.TRUE.equals(p.select())) {
      jooq().execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.select())) {
      jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (Boolean.TRUE.equals(p.insert())) {
      jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.insert())) {
      jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (Boolean.TRUE.equals(p.update())) {
      jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.update())) {
      jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (Boolean.TRUE.equals(p.delete())) {
      jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.delete())) {
      jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
  }

  public List<TablePermission> getPermissions(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      return systemPermissions(roleName);
    }
    String fullRole = fullRoleName(schemaName, roleName);
    String rlsFullRole = fullRoleName(schemaName, RLS_ROLE_PREFIX + roleName);
    List<TablePermission> result = new ArrayList<>();
    try {
      Result<Record> rows =
          jooq()
              .fetch(
                  """
                      SELECT g.table_name,
                        bool_or(g.privilege_type = 'SELECT') AS can_select,
                        bool_or(g.privilege_type = 'INSERT') AS can_insert,
                        bool_or(g.privilege_type = 'UPDATE') AS can_update,
                        bool_or(g.privilege_type = 'DELETE') AS can_delete,
                        bool_or(g.grantee = {1}) AS is_row_level
                       FROM information_schema.role_table_grants g
                       WHERE g.grantee IN ({0}, {1}) AND g.table_schema = {2}
                       GROUP BY g.table_name""",
                  inline(fullRole), inline(rlsFullRole), inline(schemaName));
      for (Record row : rows) {
        Boolean select = Boolean.TRUE.equals(row.get("can_select", Boolean.class)) ? true : null;
        Boolean insert = Boolean.TRUE.equals(row.get("can_insert", Boolean.class)) ? true : null;
        Boolean update = Boolean.TRUE.equals(row.get("can_update", Boolean.class)) ? true : null;
        Boolean delete = Boolean.TRUE.equals(row.get("can_delete", Boolean.class)) ? true : null;
        Boolean rowLevel =
            Boolean.TRUE.equals(row.get("is_row_level", Boolean.class)) ? true : null;
        result.add(
            new TablePermission(row.get("table_name", String.class))
                .select(select)
                .insert(insert)
                .update(update)
                .delete(delete)
                .rowLevel(rowLevel));
      }
    } catch (Exception e) {
      throw new SqlMolgenisException("Failed to get permissions for " + roleName, e);
    }
    return result;
  }

  public Role getRole(String schemaName, String roleName) {
    boolean system = isSystemRole(roleName);
    return new Role(roleName, system, getPermissions(schemaName, roleName));
  }

  public List<Role> getRoles(String schemaName) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    String rlsRolePrefix = rolePrefix + RLS_ROLE_PREFIX;
    List<String> roleNames =
        jooq()
            .select(field(ROLNAME))
            .from(PG_ROLES)
            .where(
                field(ROLNAME)
                    .like(inline(rolePrefix + "%"))
                    .and(field(ROLNAME).notLike(inline(rlsRolePrefix + "%"))))
            .fetch(r -> r.get(ROLNAME, String.class).substring(rolePrefix.length()));
    List<Role> result = new ArrayList<>();
    for (String roleName : roleNames) {
      result.add(getRole(schemaName, roleName));
    }
    return result;
  }

  public List<TablePermission> getTablePermissionsForActiveUser(String schemaName) {
    String activeUser = database.getActiveUser();
    SqlSchema schema = database.getSchema(schemaName);
    List<String> roleNames = schema.getInheritedRolesForUser(activeUser);
    if (roleNames.isEmpty()) return List.of();

    String customRoleName =
        roleNames.stream()
            .filter(r -> !r.startsWith(RLS_ROLE_PREFIX) && !isSystemRole(r))
            .findFirst()
            .orElse(null);

    TablePermission systemWildcard =
        roleNames.stream()
            .filter(this::isSystemRole)
            .flatMap(r -> systemPermissions(r).stream())
            .filter(p -> "*".equals(p.table()) && hasAnyPermission(p))
            .findFirst()
            .orElse(null);

    Map<String, TablePermission> result = new LinkedHashMap<>();

    if (customRoleName != null) {
      getPermissions(schemaName, customRoleName).stream()
          .filter(SqlRoleManager::hasAnyPermission)
          .forEach(p -> result.put(p.table(), p));
    }

    if (systemWildcard != null) {
      boolean systemHasDml = hasAnyDml(systemWildcard);
      TablePermission sw = systemWildcard;
      result.replaceAll(
          (table, p) ->
              new TablePermission(table)
                  .select(orBool(p.select(), sw.select()))
                  .insert(orBool(p.insert(), sw.insert()))
                  .update(orBool(p.update(), sw.update()))
                  .delete(orBool(p.delete(), sw.delete()))
                  .rowLevel(systemHasDml ? null : p.isRowLevel()));
      for (String tableName : schema.getTableNames()) {
        if (!result.containsKey(tableName)) {
          TablePermission tp =
              new TablePermission(tableName)
                  .select(sw.select())
                  .insert(sw.insert())
                  .update(sw.update())
                  .delete(sw.delete());
          if (hasAnyPermission(tp)) result.put(tableName, tp);
        }
      }
    }

    return new ArrayList<>(result.values());
  }

  private static boolean hasAnyPermission(TablePermission p) {
    return Boolean.TRUE.equals(p.select())
        || Boolean.TRUE.equals(p.insert())
        || Boolean.TRUE.equals(p.update())
        || Boolean.TRUE.equals(p.delete());
  }

  private static boolean hasAnyDml(TablePermission p) {
    return Boolean.TRUE.equals(p.insert())
        || Boolean.TRUE.equals(p.update())
        || Boolean.TRUE.equals(p.delete());
  }

  private static Boolean orBool(Boolean a, Boolean b) {
    return Boolean.TRUE.equals(a) || Boolean.TRUE.equals(b) ? true : null;
  }

  public boolean isSystemRole(String roleName) {
    return Privileges.isSystemRole(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  private List<TablePermission> systemPermissions(String roleName) {
    if (roleName.equals(Privileges.EXISTS.toString())
        || roleName.equals(Privileges.RANGE.toString())
        || roleName.equals(Privileges.AGGREGATOR.toString())
        || roleName.equals(Privileges.COUNT.toString())) {
      return List.of(new TablePermission("*"));
    } else if (roleName.equals(Privileges.VIEWER.toString())) {
      return List.of(new TablePermission("*").select(true));
    } else if (roleName.equals(Privileges.EDITOR.toString())
        || roleName.equals(Privileges.MANAGER.toString())
        || roleName.equals(Privileges.OWNER.toString())) {
      return List.of(new TablePermission("*").select(true).insert(true).update(true).delete(true));
    }
    return List.of();
  }
}
