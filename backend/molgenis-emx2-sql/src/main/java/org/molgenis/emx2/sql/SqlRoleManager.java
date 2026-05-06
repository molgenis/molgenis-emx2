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
import java.util.Set;
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

  private enum RlsPolicy {
    VIEWER_BYPASS("mg_roles_viewer_bypass", "SELECT"),
    EDITOR_BYPASS("mg_roles_editor_bypass", "ALL"),
    TABLE_GRANT_BYPASS("mg_roles_table_grant_bypass", "ALL"),
    ROW_MATCH("mg_roles_row_match", "ALL");

    private final String policyName;
    private final String command;

    RlsPolicy(String policyName, String command) {
      this.policyName = policyName;
      this.command = command;
    }
  }

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
    validateRoleName(fullRole);
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

  private void validateRoleName(String roleName) {
    if (roleName.getBytes(UTF_8).length > PG_MAX_ID_LENGTH) {
      throw new MolgenisException(
          "Role name '" + roleName + "' is too long, it exceeds PostgreSQL's 63-byte limit");
    }
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
            deleteRoleAndGrants(jooq, schemaName, fullRole, tableNames);
            // Also clean up the companion RLS role if it exists
            if (jooq.fetchExists(
                jooq.select().from(PG_ROLES).where(field(ROLNAME).eq(inline(rlsFullRole))))) {
              deleteRoleAndGrants(jooq, schemaName, rlsFullRole, tableNames);
            }
          } finally {
            db.setActiveUser(currentUser);
          }
        });
    database.getListener().onSchemaChange();
  }

  private static void deleteRoleAndGrants(
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
    TableMetadata tableMetadata = requireRootTable(schemaName, tableName);
    String grantRoleName;
    if (Boolean.TRUE.equals(permission.isRowLevel())) {
      grantRoleName = RLS_ROLE_PREFIX + roleName;
      createRlsRole(schemaName, roleName);
    } else {
      grantRoleName = roleName;
    }
    String fullRole = fullRoleName(schemaName, grantRoleName);
    for (TableMetadata tableInTree : tableMetadata.getInheritanceTree()) {
      applyPgGrants(schemaName, fullRole, tableInTree.getTableName(), permission);
    }
    if (Boolean.TRUE.equals(permission.isRowLevel())) {
      enableRowLevelSecurityOnTree(schemaName, tableMetadata);
    }
    database.getListener().onSchemaChange();
  }

  private TableMetadata requireRootTable(String schemaName, String tableName) {
    if (!database.getSchema(schemaName).getTableNames().contains(tableName)) {
      throw new MolgenisException("Table does not exist: " + tableName);
    }
    TableMetadata meta = database.getSchema(schemaName).getMetadata().getTableMetadata(tableName);
    if (meta.getInheritName() != null) {
      throw new MolgenisException(
          "Cannot grant custom permission on inherited table '"
              + tableName
              + "': grant on the root table '"
              + meta.getRootTable().getTableName()
              + "' instead. Permissions on root tables propagate to all subclasses.");
    }
    return meta;
  }

  private void createRlsRole(String schemaName, String roleName) {
    String rlsRoleName = RLS_ROLE_PREFIX + roleName;
    validateRoleName(rlsRoleName);

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

  private void enableRowLevelSecurityOnTree(String schemaName, TableMetadata root) {
    if (root.getColumn(MG_ROLES) == null) {
      root.add(column(MG_ROLES).setType(STRING_ARRAY));
    }
    enableRowLevelSecurityOnTable(schemaName, root.getTableName(), rowRoleMatch(schemaName));
    for (TableMetadata subclass : root.getSubclassTables()) {
      enableRowLevelSecurityOnTable(
          schemaName, subclass.getTableName(), rowRoleMatchViaRoot(schemaName, root, subclass));
    }
  }

  private void enableRowLevelSecurityOnTable(
      String schemaName, String tableName, String rowMatchExpression) {
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    jooq().execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", jooqTable);
    dropRlsPolicies(jooqTable);

    createPolicy(
        jooqTable, RlsPolicy.VIEWER_BYPASS, systemRoleMember(schemaName, Privileges.VIEWER));
    createPolicy(
        jooqTable, RlsPolicy.EDITOR_BYPASS, systemRoleMember(schemaName, Privileges.EDITOR));
    createPolicy(jooqTable, RlsPolicy.TABLE_GRANT_BYPASS, tableGrantBypass(schemaName, tableName));
    createPolicy(jooqTable, RlsPolicy.ROW_MATCH, rowMatchExpression);
  }

  private void createPolicy(org.jooq.Table<?> jooqTable, RlsPolicy policy, String expression) {
    String sql =
        "CREATE POLICY "
            + policy.policyName
            + " ON {0} FOR "
            + policy.command
            + " USING ("
            + expression
            + ")";
    if (!"SELECT".equals(policy.command)) {
      sql += " WITH CHECK (" + expression + ")";
    }
    jooq().execute(sql, jooqTable);
  }

  static void dropRlsPolicies(DSLContext jooq, org.jooq.Table<?> jooqTable) {
    for (RlsPolicy policy : RlsPolicy.values()) {
      jooq.execute("DROP POLICY IF EXISTS " + policy.policyName + " ON {0}", jooqTable);
    }
  }

  private void dropRlsPolicies(org.jooq.Table<?> jooqTable) {
    dropRlsPolicies(jooq(), jooqTable);
  }

  private String systemRoleMember(String schemaName, Privileges role) {
    return "pg_has_role(current_user, "
        + jooq().render(inline(fullRoleName(schemaName, role.toString())))
        + ", 'member')";
  }

  private String tableGrantBypass(String schemaName, String tableName) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    String rlsRolePrefix = rolePrefix + RLS_ROLE_PREFIX;
    String qualifiedTable = jooq().render(name(schemaName, tableName));
    String systemRoles =
        Arrays.stream(Privileges.values())
            .map(p -> jooq().render(inline(fullRoleName(schemaName, p.toString()))))
            .collect(Collectors.joining(", "));
    return """
        EXISTS (
          SELECT 1
          FROM aclexplode((SELECT relacl FROM pg_class WHERE oid = %s::regclass)) ace
          JOIN pg_roles r ON r.oid = ace.grantee
          WHERE r.rolname LIKE %s
            AND r.rolname NOT LIKE %s
            AND r.rolname NOT IN (%s)
            AND pg_has_role(current_user, r.oid, 'member')
        )"""
        .formatted(
            jooq().render(inline(qualifiedTable)),
            jooq().render(inline(rolePrefix + "%")),
            jooq().render(inline(rlsRolePrefix + "%")),
            systemRoles);
  }

  private String rowRoleMatch(String schemaName) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    return "EXISTS (SELECT 1 FROM unnest(mg_roles) role_name"
        + " WHERE pg_has_role(current_user, "
        + jooq().render(inline(rolePrefix))
        + " || role_name, 'member'))";
  }

  private String rowRoleMatchViaRoot(
      String schemaName, TableMetadata root, TableMetadata subclass) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    String qualifiedRoot = jooq().render(name(schemaName, root.getTableName()));
    String pkeyJoin =
        jooq()
            .render(
                and(
                    subclass.getPrimaryKeyFields().stream()
                        .map(
                            pkeyField ->
                                field(name("root_row", pkeyField.getName()))
                                    .eq(field(name(pkeyField.getName()))))
                        .toList()));
    return "EXISTS (SELECT 1 FROM "
        + qualifiedRoot
        + " root_row, unnest(root_row.mg_roles) role_name WHERE "
        + pkeyJoin
        + " AND pg_has_role(current_user, "
        + jooq().render(inline(rolePrefix))
        + " || role_name, 'member'))";
  }

  public void revoke(String schemaName, String roleName, String tableName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot revoke permissions from system role: " + roleName);
    }
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    TableMetadata tableMetadata = requireRootTable(schemaName, tableName);
    String fullRole = fullRoleName(schemaName, roleName);
    String rlsFullRole = fullRoleName(schemaName, RLS_ROLE_PREFIX + roleName);
    boolean rlsRoleExists =
        jooq()
            .fetchExists(
                jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(rlsFullRole))));
    // Mirror grant: revoke from the entire inheritance tree.
    for (TableMetadata tableInTree : tableMetadata.getInheritanceTree()) {
      org.jooq.Table<?> jooqTable = table(name(schemaName, tableInTree.getTableName()));
      jooq().execute("REVOKE ALL ON {0} FROM {1}", jooqTable, name(fullRole));
      if (rlsRoleExists) {
        jooq().execute("REVOKE ALL ON {0} FROM {1}", jooqTable, name(rlsFullRole));
      }
    }
    disableRowLevelSecurityIfUnused(schemaName, tableMetadata);
    database.getListener().onSchemaChange();
  }

  private void disableRowLevelSecurityIfUnused(String schemaName, TableMetadata root) {
    String rlsRolePrefix = MG_ROLE_PREFIX + schemaName + "/" + RLS_ROLE_PREFIX;
    boolean anyRlsGrantRemains =
        jooq()
            .fetchExists(
                jooq()
                    .select()
                    .from("information_schema.role_table_grants")
                    .where(field("table_schema").eq(inline(schemaName)))
                    .and(field("table_name").eq(inline(root.getTableName())))
                    .and(field("grantee").like(inline(rlsRolePrefix + "%"))));
    if (anyRlsGrantRemains) return;
    for (TableMetadata tableInTree : root.getInheritanceTree()) {
      org.jooq.Table<?> jooqTable = table(name(schemaName, tableInTree.getTableName()));
      dropRlsPolicies(jooqTable);
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
    List<TablePermission> permissions = getPermissions(schemaName, roleName);
    if (!system) {
      Set<String> rootTables =
          database.getSchema(schemaName).getMetadata().getTables().stream()
              .filter(tableMeta -> tableMeta.getInheritName() == null)
              .map(TableMetadata::getTableName)
              .collect(Collectors.toSet());
      permissions =
          permissions.stream()
              .filter(permission -> rootTables.contains(permission.table()))
              .toList();
    }
    return new Role(roleName, system, permissions);
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
            .reduce(
                (a, b) ->
                    new TablePermission("*")
                        .select(orBool(a.select(), b.select()))
                        .insert(orBool(a.insert(), b.insert()))
                        .update(orBool(a.update(), b.update()))
                        .delete(orBool(a.delete(), b.delete())))
            .orElse(null);

    Map<String, TablePermission> result = new LinkedHashMap<>();

    if (customRoleName != null) {
      getPermissions(schemaName, customRoleName).stream()
          .filter(SqlRoleManager::hasAnyPermission)
          .forEach(p -> result.put(p.table(), p));
    }

    if (systemWildcard != null) {
      boolean systemHasDml = hasAnyDml(systemWildcard);
      result.replaceAll(
          (table, p) ->
              new TablePermission(table)
                  .select(orBool(p.select(), systemWildcard.select()))
                  .insert(orBool(p.insert(), systemWildcard.insert()))
                  .update(orBool(p.update(), systemWildcard.update()))
                  .delete(orBool(p.delete(), systemWildcard.delete()))
                  .rowLevel(systemHasDml ? null : p.isRowLevel()));
      for (String tableName : schema.getTableNames()) {
        if (!result.containsKey(tableName)) {
          TablePermission tp =
              new TablePermission(tableName)
                  .select(systemWildcard.select())
                  .insert(systemWildcard.insert())
                  .update(systemWildcard.update())
                  .delete(systemWildcard.delete());
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
