package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING_ARRAY;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Param;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.*;

public class SqlRoleManager {

  public static final String PG_ROLES = "pg_roles";
  public static final String ROLNAME = "rolname";
  public static final int PG_MAX_ID_LENGTH = 63;

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
    database.tx( // we need to lift to admin to drop a role
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            for (String tableName : database.getSchema(schemaName).getTableNames()) {
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

          } finally {
            db.setActiveUser(currentUser);
          }
        });
    database.getListener().onSchemaChange();
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
    String fullRole = fullRoleName(schemaName, roleName);
    applyPgGrants(schemaName, fullRole, tableName, permission);
    if (Boolean.TRUE.equals(permission.isRowLevel())) {
      enableRowLevelSecurity(schemaName, tableName);
    }
    database.getListener().onSchemaChange();
  }

  private void enableRowLevelSecurity(String schemaName, String tableName) {
    TableMetadata tableMetadata =
        database.getSchema(schemaName).getMetadata().getTableMetadata(tableName);
    if (tableMetadata.getColumn(MG_ROLES) == null) {
      tableMetadata.add(column(MG_ROLES).setType(STRING_ARRAY));
    }
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    jooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", jooqTable);

    Param<String> viewerRole = inline(fullRoleName(schemaName, Privileges.VIEWER.toString()));
    Param<String> editorRole = inline(fullRoleName(schemaName, Privileges.EDITOR.toString()));
    Param<String> rolePrefix = inline(MG_ROLE_PREFIX + schemaName + "/");

    // SELECT: Viewer+ can see all rows; custom roles see only rows where they are in mg_roles.
    // Assign the Viewer system role to a user to grant them visibility of all rows.
    jooq().execute("DROP POLICY IF EXISTS mg_roles_select_policy ON {0}", jooqTable);
    jooq()
        .execute(
            "CREATE POLICY mg_roles_select_policy ON {0} FOR SELECT USING ("
                + "pg_has_role(current_user, {1}, 'member') "
                + "OR EXISTS ("
                + "  SELECT 1 FROM unnest(mg_roles) r"
                + "  WHERE pg_has_role(current_user, {2} || r, 'member')"
                + "))",
            jooqTable, viewerRole, rolePrefix);

    // DML: Editor+ can mutate all rows; custom roles can only mutate rows where they are in
    // mg_roles. Viewer-only users cannot perform DML on row-level secured tables.
    jooq().execute("DROP POLICY IF EXISTS mg_roles_dml_policy ON {0}", jooqTable);
    jooq()
        .execute(
            "CREATE POLICY mg_roles_dml_policy ON {0} FOR ALL USING ("
                + "pg_has_role(current_user, {1}, 'member') "
                + "OR EXISTS ("
                + "  SELECT 1 FROM unnest(mg_roles) r"
                + "  WHERE pg_has_role(current_user, {2} || r, 'member')"
                + ")) WITH CHECK ("
                + "pg_has_role(current_user, {1}, 'member') "
                + "OR EXISTS ("
                + "  SELECT 1 FROM unnest(mg_roles) r"
                + "  WHERE pg_has_role(current_user, {2} || r, 'member')"
                + "))",
            jooqTable, editorRole, rolePrefix);
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
    disableRowLevelSecurityIfUnused(schemaName, tableName);
    database.getListener().onSchemaChange();
  }

  private void disableRowLevelSecurityIfUnused(String schemaName, String tableName) {
    if (!hasRowLevelSecurity(schemaName, tableName)) return;
    boolean anyRowLevelGrantRemains =
        getRoles(schemaName).stream()
            .filter(role -> !role.isSystemRole())
            .flatMap(role -> getPermissions(schemaName, role.name()).stream())
            .anyMatch(p -> tableName.equals(p.table()) && Boolean.TRUE.equals(p.isRowLevel()));
    if (!anyRowLevelGrantRemains) {
      org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
      jooq().execute("DROP POLICY IF EXISTS mg_roles_select_policy ON {0}", jooqTable);
      jooq().execute("DROP POLICY IF EXISTS mg_roles_dml_policy ON {0}", jooqTable);
      jooq().execute("ALTER TABLE {0} DISABLE ROW LEVEL SECURITY", jooqTable);
    }
  }

  private boolean hasRowLevelSecurity(String schemaName, String tableName) {
    return jooq()
        .fetchExists(
            jooq()
                .select()
                .from("pg_policies")
                .where(
                    field("schemaname")
                        .eq(inline(schemaName))
                        .and(field("tablename").eq(inline(tableName)))
                        .and(
                            field("policyname")
                                .in(
                                    inline("mg_roles_select_policy"),
                                    inline("mg_roles_dml_policy")))));
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
                        bool_or(p.policyname IS NOT NULL) AS is_row_level
                       FROM information_schema.role_table_grants g
                       LEFT JOIN pg_policies p
                         ON p.schemaname = g.table_schema
                         AND p.tablename = g.table_name
                         AND p.policyname IN ('mg_roles_select_policy', 'mg_roles_dml_policy')
                       WHERE g.grantee = {0} AND g.table_schema = {1}
                       GROUP BY g.table_name""",
                  inline(fullRole), inline(schemaName));
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
    List<String> roleNames =
        jooq()
            .select(field(ROLNAME))
            .from(PG_ROLES)
            .where(field(ROLNAME).like(inline(rolePrefix + "%")))
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

    Map<String, TablePermission> merged = new LinkedHashMap<>();
    for (String roleName : roleNames) {
      for (TablePermission p : getPermissions(schemaName, roleName)) {
        if (hasAnyPermission(p)) {
          merged.merge(p.table(), p, SqlRoleManager::mergePermissions);
        }
      }
    }
    expandWildcard(merged, schema.getTableNames());
    return new ArrayList<>(merged.values());
  }

  private static void expandWildcard(
      Map<String, TablePermission> permissions, Collection<String> tableNames) {
    TablePermission wildcard = permissions.remove("*");
    if (wildcard == null) return;
    for (String tableName : tableNames) {
      permissions.merge(
          tableName,
          new TablePermission(tableName)
              .select(wildcard.select())
              .insert(wildcard.insert())
              .update(wildcard.update())
              .delete(wildcard.delete()),
          SqlRoleManager::mergePermissions);
    }
  }

  private static boolean hasAnyPermission(TablePermission p) {
    return Boolean.TRUE.equals(p.select())
        || Boolean.TRUE.equals(p.insert())
        || Boolean.TRUE.equals(p.update())
        || Boolean.TRUE.equals(p.delete());
  }

  private static TablePermission mergePermissions(TablePermission a, TablePermission b) {
    return new TablePermission(a.table())
        .select(Boolean.TRUE.equals(a.select()) || Boolean.TRUE.equals(b.select()) ? true : null)
        .insert(Boolean.TRUE.equals(a.insert()) || Boolean.TRUE.equals(b.insert()) ? true : null)
        .update(Boolean.TRUE.equals(a.update()) || Boolean.TRUE.equals(b.update()) ? true : null)
        .delete(Boolean.TRUE.equals(a.delete()) || Boolean.TRUE.equals(b.delete()) ? true : null)
        .rowLevel(mergeRowLevel(a, b));
  }

  private static Boolean mergeRowLevel(TablePermission a, TablePermission b) {
    boolean aUnrestrictedDml =
        Boolean.TRUE.equals(a.insert()) && !Boolean.TRUE.equals(a.isRowLevel());
    boolean bUnrestrictedDml =
        Boolean.TRUE.equals(b.insert()) && !Boolean.TRUE.equals(b.isRowLevel());
    if (aUnrestrictedDml || bUnrestrictedDml) {
      return null;
    }
    return Boolean.TRUE.equals(a.isRowLevel()) || Boolean.TRUE.equals(b.isRowLevel()) ? true : null;
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
