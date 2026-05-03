package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.*;

public class SqlRoleManager {

  public static final String PG_ROLES = "pg_roles";
  public static final String ROLNAME = "rolname";
  public static final int PG_MAX_ID_LENGTH = 63;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    database.getListener().onSchemaChange();
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
    database.getListener().onSchemaChange();
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
                      SELECT table_name,
                        bool_or(privilege_type = 'SELECT') AS can_select,
                        bool_or(privilege_type = 'INSERT') AS can_insert,
                        bool_or(privilege_type = 'UPDATE') AS can_update,
                        bool_or(privilege_type = 'DELETE') AS can_delete
                       FROM information_schema.role_table_grants
                       WHERE grantee = {0} AND table_schema = {1}
                       GROUP BY table_name""",
                  inline(fullRole), inline(schemaName));
      for (Record row : rows) {
        String tableName = row.get("table_name", String.class);
        Boolean select = Boolean.TRUE.equals(row.get("can_select", Boolean.class)) ? true : null;
        Boolean insert = Boolean.TRUE.equals(row.get("can_insert", Boolean.class)) ? true : null;
        Boolean update = Boolean.TRUE.equals(row.get("can_update", Boolean.class)) ? true : null;
        Boolean delete = Boolean.TRUE.equals(row.get("can_delete", Boolean.class)) ? true : null;
        result.add(
            new TablePermission(tableName)
                .select(select)
                .insert(insert)
                .update(update)
                .delete(delete));
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
        .delete(Boolean.TRUE.equals(a.delete()) || Boolean.TRUE.equals(b.delete()) ? true : null);
  }

  public void createRole(Schema schema, String roleName, String description) {
    String schemaName = schema.getName();
    if (roleName == null || roleName.isEmpty()) {
      throw new MolgenisException("Role name must not be empty");
    }
    if (roleName.toUpperCase().startsWith("MG_")) {
      throw new MolgenisException("Role name must not start with reserved prefix MG_: " + roleName);
    }
    String fullRole = fullRoleName(schemaName, roleName);
    if (fullRole.getBytes(UTF_8).length > PG_MAX_ID_LENGTH) {
      throw new MolgenisException(
          "Role name '"
              + roleName
              + "' is too long: combined identifier exceeds PostgreSQL 63-byte limit");
    }
    if (jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullRole))))) {
      throw new MolgenisException("Role already exists: " + roleName);
    }
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            jooq.execute("CREATE ROLE {0} NOLOGIN", name(fullRole));
            jooq.execute("COMMENT ON ROLE {0} IS {1}", name(fullRole), inline("{}"));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  public void deleteRole(Schema schema, String roleName) {
    String schemaName = schema.getName();
    String fullRole = fullRoleName(schemaName, roleName);
    if (!jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullRole))))) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            jooq.execute("DROP OWNED BY {0}", name(fullRole));
            jooq.execute("DROP ROLE IF EXISTS {0}", name(fullRole));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  public List<String> listRoles(Schema schema) {
    String schemaName = schema.getName();
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    return jooq()
        .fetch(
            "SELECT a.rolname FROM pg_authid a "
                + "LEFT JOIN pg_shdescription d ON d.objoid = a.oid AND d.classoid = 'pg_authid'::regclass "
                + "WHERE a.rolname LIKE {0} "
                + "ORDER BY a.rolname",
            inline(rolePrefix + "%"))
        .stream()
        .map(r -> r.get("rolname", String.class).substring(rolePrefix.length()))
        .filter(roleName -> !isSystemRole(roleName))
        .toList();
  }

  public void grantRoleToUser(Schema schema, String roleName, String username) {
    String schemaName = schema.getName();
    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + username;
    if (!jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullRole))))) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    if (!jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullUser))))) {
      throw new MolgenisException("User does not exist: " + username);
    }
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    List<String> existingCustomRoles =
        jooq()
            .fetch(
                "SELECT r.rolname FROM pg_auth_members am "
                    + "JOIN pg_roles r ON r.oid = am.roleid "
                    + "JOIN pg_roles m ON m.oid = am.member "
                    + "WHERE m.rolname = {0} AND r.rolname LIKE {1} AND r.rolname <> {2}",
                inline(fullUser), inline(rolePrefix + "%"), inline(fullRole))
            .stream()
            .map(r -> r.get(0, String.class).substring(rolePrefix.length()))
            .filter(name -> !isSystemRole(name))
            .toList();
    if (!existingCustomRoles.isEmpty()) {
      throw new MolgenisException(
          "User '"
              + username
              + "' already holds a custom role in schema '"
              + schemaName
              + "': one custom role per schema is allowed");
    }
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            ((SqlDatabase) db)
                .getJooq()
                .execute("GRANT {0} TO {1}", name(fullRole), name(fullUser));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  public void revokeRoleFromUser(Schema schema, String roleName, String username) {
    String schemaName = schema.getName();
    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + username;
    if (!jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullRole))))) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    boolean isMember =
        !jooq()
            .fetch(
                "SELECT 1 FROM pg_auth_members am "
                    + "JOIN pg_roles r ON r.oid = am.roleid "
                    + "JOIN pg_roles m ON m.oid = am.member "
                    + "WHERE m.rolname = {0} AND r.rolname = {1}",
                inline(fullUser), inline(fullRole))
            .isEmpty();
    if (!isMember) {
      throw new MolgenisException(
          "User '" + username + "' is not a member of role '" + roleName + "'");
    }
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            ((SqlDatabase) db)
                .getJooq()
                .execute("REVOKE {0} FROM {1}", name(fullRole), name(fullUser));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  public boolean isSystemRole(String roleName) {
    return Privileges.isSystemRole(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  public void setPermissions(Schema schema, String roleName, PermissionSet permissions) {
    String schemaName = schema.getName();
    String fullRole = fullRoleName(schemaName, roleName);
    if (!jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullRole))))) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    String json = serializePermissionSet(permissions);
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext txJooq = ((SqlDatabase) db).getJooq();
            // schema-level lock: serializes concurrent setPermissions on the same schema
            txJooq.execute(
                "SELECT pg_advisory_xact_lock((SELECT oid::bigint FROM pg_namespace WHERE nspname = {0}))",
                inline(schemaName));
            txJooq.execute("COMMENT ON ROLE {0} IS {1}", name(fullRole), inline(json));
            Map<String, PermissionSet> allPermissions = buildAllPermissionsSnapshot(txJooq, schema);
            applyColumnLifecycle(txJooq, schemaName, schema.getTableNames(), allPermissions);
            applyColumnGrants(txJooq, schemaName, schema.getTableNames(), allPermissions);
            applyPolicies(txJooq, schemaName, schema.getTableNames(), allPermissions);
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  private Map<String, PermissionSet> buildAllPermissionsSnapshot(DSLContext txJooq, Schema schema) {
    Map<String, PermissionSet> snapshot = new LinkedHashMap<>();
    for (String role : listRoles(schema)) {
      snapshot.put(role, getPermissionsViaTx(txJooq, schema.getName(), role));
    }
    return snapshot;
  }

  private PermissionSet getPermissionsViaTx(DSLContext txJooq, String schemaName, String roleName) {
    String fullRole = fullRoleName(schemaName, roleName);
    Record commentRecord =
        txJooq.fetchOne(
            "SELECT d.description FROM pg_authid a "
                + "LEFT JOIN pg_shdescription d ON d.objoid = a.oid AND d.classoid = 'pg_authid'::regclass "
                + "WHERE a.rolname = {0}",
            inline(fullRole));
    String comment = commentRecord == null ? null : commentRecord.get("description", String.class);
    if (comment == null || comment.isBlank() || comment.equals("{}")) {
      return new PermissionSet();
    }
    return deserializePermissionSet(comment);
  }

  private void applyColumnLifecycle(
      DSLContext txJooq,
      String schemaName,
      Collection<String> tableNames,
      Map<String, PermissionSet> allPermissions) {
    for (String tableName : tableNames) {
      boolean needsOwnerColumn =
          anyRoleHasScopeOnTable(allPermissions, tableName, this::hasOwnScope);
      boolean needsGroupsColumn =
          anyRoleHasScopeOnTable(allPermissions, tableName, this::hasGroupScope);
      boolean ownerExists = columnExists(txJooq, schemaName, tableName, MG_OWNER_COLUMN);
      boolean groupsExists = columnExists(txJooq, schemaName, tableName, MG_GROUPS_COLUMN);
      if (needsOwnerColumn && !ownerExists) {
        addOwnerColumn(txJooq, schemaName, tableName);
      } else if (!needsOwnerColumn && ownerExists) {
        dropColumn(txJooq, schemaName, tableName, MG_OWNER_COLUMN);
      }
      if (needsGroupsColumn && !groupsExists) {
        addGroupsColumn(txJooq, schemaName, tableName);
      } else if (!needsGroupsColumn && groupsExists) {
        dropColumn(txJooq, schemaName, tableName, MG_GROUPS_COLUMN);
      }
    }
  }

  private void applyColumnGrants(
      DSLContext txJooq,
      String schemaName,
      Collection<String> tableNames,
      Map<String, PermissionSet> allPermissions) {
    for (Map.Entry<String, PermissionSet> roleEntry : allPermissions.entrySet()) {
      String roleShortName = roleEntry.getKey();
      PermissionSet rolePerms = roleEntry.getValue();
      String fullRole = fullRoleName(schemaName, roleShortName);
      for (String tableName : tableNames) {
        PermissionSet.TablePermissions tablePerms = rolePerms.getTables().get(tableName);
        applyColumnGrantsForRoleAndTable(
            txJooq, schemaName, tableName, fullRole, tablePerms, rolePerms);
      }
    }
  }

  private void applyColumnGrantsForRoleAndTable(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      PermissionSet.TablePermissions tablePerms,
      PermissionSet rolePerms) {
    applyColumnGrantsForVerb(
        txJooq, schemaName, tableName, fullRole, "INSERT", tablePerms, rolePerms);
    applyColumnGrantsForVerb(
        txJooq, schemaName, tableName, fullRole, "UPDATE", tablePerms, rolePerms);
  }

  private void applyColumnGrantsForVerb(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      String verb,
      PermissionSet.TablePermissions tablePerms,
      PermissionSet rolePerms) {
    boolean hasScope =
        tablePerms != null
            && (("INSERT".equals(verb) && tablePerms.getInsert() != SelectScope.NONE)
                || ("UPDATE".equals(verb) && tablePerms.getUpdate() != SelectScope.NONE));

    List<String> existingGrants =
        fetchCurrentColumnGrants(txJooq, schemaName, tableName, fullRole, verb);

    if (!hasScope) {
      if (!existingGrants.isEmpty()) {
        revokeColumnGrants(txJooq, schemaName, tableName, fullRole, verb, existingGrants);
      }
      return;
    }

    List<String> regularColumns = fetchRegularColumns(txJooq, schemaName, tableName);
    List<String> desiredColumns = new ArrayList<>(regularColumns);
    if (rolePerms.isChangeOwner() && columnExists(txJooq, schemaName, tableName, MG_OWNER_COLUMN)) {
      desiredColumns.add(MG_OWNER_COLUMN);
    }
    if (rolePerms.isChangeGroup()
        && columnExists(txJooq, schemaName, tableName, MG_GROUPS_COLUMN)) {
      desiredColumns.add(MG_GROUPS_COLUMN);
    }

    List<String> toGrant = new ArrayList<>(desiredColumns);
    toGrant.removeAll(existingGrants);
    List<String> toRevoke = new ArrayList<>(existingGrants);
    toRevoke.removeAll(desiredColumns);

    if (!toGrant.isEmpty()) {
      grantColumnPrivileges(txJooq, schemaName, tableName, fullRole, verb, toGrant);
    }
    if (!toRevoke.isEmpty()) {
      revokeColumnGrants(txJooq, schemaName, tableName, fullRole, verb, toRevoke);
    }
  }

  private List<String> fetchCurrentColumnGrants(
      DSLContext txJooq, String schemaName, String tableName, String fullRole, String verb) {
    return txJooq
        .fetch(
            "SELECT column_name FROM information_schema.column_privileges "
                + "WHERE table_schema = {0} AND table_name = {1} "
                + "AND grantee = {2} AND privilege_type = {3}",
            inline(schemaName), inline(tableName), inline(fullRole), inline(verb))
        .stream()
        .map(r -> r.get("column_name", String.class))
        .toList();
  }

  private List<String> fetchRegularColumns(DSLContext txJooq, String schemaName, String tableName) {
    return txJooq
        .fetch(
            "SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = {0} AND table_name = {1} "
                + "AND column_name NOT LIKE 'mg_%'",
            inline(schemaName), inline(tableName))
        .stream()
        .map(r -> r.get("column_name", String.class))
        .toList();
  }

  private void grantColumnPrivileges(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      String verb,
      List<String> columns) {
    String colList =
        columns.stream().map(col -> txJooq.render(name(col))).collect(Collectors.joining(", "));
    txJooq.execute(
        "GRANT " + verb + " (" + colList + ") ON {0} TO {1}",
        table(name(schemaName, tableName)),
        name(fullRole));
  }

  private void revokeColumnGrants(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      String verb,
      List<String> columns) {
    String colList =
        columns.stream().map(col -> txJooq.render(name(col))).collect(Collectors.joining(", "));
    txJooq.execute(
        "REVOKE " + verb + " (" + colList + ") ON {0} FROM {1}",
        table(name(schemaName, tableName)),
        name(fullRole));
  }

  private static final String POLICY_PREFIX = "MG_P_";
  private static final String MOLGENIS_SCHEMA = "MOLGENIS";

  private static final String SELECT_VERB = "SELECT";
  private static final String INSERT_VERB = "INSERT";
  private static final String UPDATE_VERB = "UPDATE";
  private static final String DELETE_VERB = "DELETE";

  private static final String USING_TRUE = "(true)";
  private static final String USING_OWN = "(mg_owner = current_user)";
  private static final String WITH_CHECK_TRUE = "(true)";

  private void applyPolicies(
      DSLContext txJooq,
      String schemaName,
      Collection<String> tableNames,
      Map<String, PermissionSet> allPermissions) {
    for (Map.Entry<String, PermissionSet> roleEntry : allPermissions.entrySet()) {
      String roleShortName = roleEntry.getKey();
      PermissionSet rolePerms = roleEntry.getValue();
      String fullRole = fullRoleName(schemaName, roleShortName);
      for (String tableName : tableNames) {
        PermissionSet.TablePermissions tablePerms = rolePerms.getTables().get(tableName);
        applyPoliciesForRoleAndTable(txJooq, schemaName, tableName, fullRole, tablePerms);
      }
    }
  }

  private void applyPoliciesForRoleAndTable(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      PermissionSet.TablePermissions tablePerms) {
    applyVerbPolicy(
        txJooq,
        schemaName,
        tableName,
        fullRole,
        SELECT_VERB,
        tablePerms == null ? SelectScope.NONE : tablePerms.getSelect());
    applyVerbPolicy(
        txJooq,
        schemaName,
        tableName,
        fullRole,
        INSERT_VERB,
        tablePerms == null ? SelectScope.NONE : tablePerms.getInsert());
    applyVerbPolicy(
        txJooq,
        schemaName,
        tableName,
        fullRole,
        UPDATE_VERB,
        tablePerms == null ? SelectScope.NONE : tablePerms.getUpdate());
    applyVerbPolicy(
        txJooq,
        schemaName,
        tableName,
        fullRole,
        DELETE_VERB,
        tablePerms == null ? SelectScope.NONE : tablePerms.getDelete());
  }

  private void applyVerbPolicy(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      String verb,
      SelectScope scope) {
    String policyName = buildPolicyName(fullRole, tableName, verb);
    txJooq.execute(
        "DROP POLICY IF EXISTS {0} ON {1}", name(policyName), table(name(schemaName, tableName)));
    boolean useColumnLevelGrant = INSERT_VERB.equals(verb) || UPDATE_VERB.equals(verb);
    if (isViewModeScope(scope)) {
      ensureRlsEnabled(txJooq, schemaName, tableName);
      if (!useColumnLevelGrant) {
        grantOrRevokeTableVerb(txJooq, schemaName, tableName, fullRole, verb, true);
      }
      emitPolicy(txJooq, schemaName, tableName, fullRole, verb, policyName, SelectScope.ALL);
      return;
    }
    if (scope == SelectScope.NONE) {
      if (!useColumnLevelGrant) {
        grantOrRevokeTableVerb(txJooq, schemaName, tableName, fullRole, verb, false);
      }
      return;
    }
    ensureRlsEnabled(txJooq, schemaName, tableName);
    if (!useColumnLevelGrant) {
      grantOrRevokeTableVerb(txJooq, schemaName, tableName, fullRole, verb, true);
    }
    emitPolicy(txJooq, schemaName, tableName, fullRole, verb, policyName, scope);
  }

  private void ensureRlsEnabled(DSLContext txJooq, String schemaName, String tableName) {
    boolean rlsEnabled =
        Boolean.TRUE.equals(
            txJooq
                .fetchOne(
                    "SELECT relrowsecurity FROM pg_class c "
                        + "JOIN pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE n.nspname = {0} AND c.relname = {1}",
                    inline(schemaName), inline(tableName))
                .get("relrowsecurity", Boolean.class));
    if (!rlsEnabled) {
      txJooq.execute(
          "ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", table(name(schemaName, tableName)));
      txJooq.execute(
          "ALTER TABLE {0} FORCE ROW LEVEL SECURITY", table(name(schemaName, tableName)));
    }
  }

  private void emitPolicy(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      String verb,
      String policyName,
      SelectScope scope) {
    String usingExpr = buildUsingExpr(verb, scope, schemaName);
    String withCheckExpr = buildWithCheckExpr(verb, scope, schemaName);
    if (usingExpr != null && withCheckExpr != null) {
      txJooq.execute(
          "CREATE POLICY {0} ON {1} AS PERMISSIVE FOR "
              + verb
              + " TO {2} USING "
              + usingExpr
              + " WITH CHECK "
              + withCheckExpr,
          name(policyName),
          table(name(schemaName, tableName)),
          name(fullRole));
    } else if (usingExpr != null) {
      txJooq.execute(
          "CREATE POLICY {0} ON {1} AS PERMISSIVE FOR " + verb + " TO {2} USING " + usingExpr,
          name(policyName),
          table(name(schemaName, tableName)),
          name(fullRole));
    } else if (withCheckExpr != null) {
      txJooq.execute(
          "CREATE POLICY {0} ON {1} AS PERMISSIVE FOR "
              + verb
              + " TO {2} WITH CHECK "
              + withCheckExpr,
          name(policyName),
          table(name(schemaName, tableName)),
          name(fullRole));
    }
  }

  private void grantOrRevokeTableVerb(
      DSLContext txJooq,
      String schemaName,
      String tableName,
      String fullRole,
      String verb,
      boolean grant) {
    if (grant) {
      txJooq.execute(
          "GRANT " + verb + " ON {0} TO {1}", table(name(schemaName, tableName)), name(fullRole));
    } else {
      txJooq.execute(
          "REVOKE " + verb + " ON {0} FROM {1}",
          table(name(schemaName, tableName)),
          name(fullRole));
    }
  }

  private static String buildPolicyName(String fullRole, String tableName, String verb) {
    String policyName = POLICY_PREFIX + fullRole + "_" + tableName + "_" + verb;
    if (policyName.getBytes(UTF_8).length > PG_MAX_ID_LENGTH) {
      throw new MolgenisException(
          "Policy name exceeds PostgreSQL 63-byte limit for role='"
              + fullRole
              + "', table='"
              + tableName
              + "', verb='"
              + verb
              + "'");
    }
    return policyName;
  }

  private static boolean isViewModeScope(SelectScope scope) {
    return scope == SelectScope.EXISTS
        || scope == SelectScope.COUNT
        || scope == SelectScope.RANGE
        || scope == SelectScope.AGGREGATE;
  }

  private String buildUsingExpr(String verb, SelectScope scope, String schemaName) {
    if (INSERT_VERB.equals(verb)) {
      return null;
    }
    return switch (scope) {
      case ALL -> USING_TRUE;
      case OWN -> USING_OWN;
      case GROUP -> groupUsingExpr(schemaName);
      default -> null;
    };
  }

  private String buildWithCheckExpr(String verb, SelectScope scope, String schemaName) {
    if (SELECT_VERB.equals(verb) || DELETE_VERB.equals(verb)) {
      return null;
    }
    return switch (scope) {
      case ALL -> WITH_CHECK_TRUE;
      case OWN -> USING_OWN;
      case GROUP -> buildGroupWithCheckExpr(verb, schemaName);
      default -> null;
    };
  }

  private String groupUsingExpr(String schemaName) {
    return "(mg_groups && \"" + MOLGENIS_SCHEMA + "\".current_user_groups('" + schemaName + "'))";
  }

  private String buildGroupWithCheckExpr(String verb, String schemaName) {
    String groupCheck = groupUsingExpr(schemaName);
    if (INSERT_VERB.equals(verb)) {
      return "("
          + groupCheck.substring(1, groupCheck.length() - 1)
          + " AND cardinality(mg_groups) >= 1)";
    }
    return groupCheck;
  }

  private boolean anyRoleHasScopeOnTable(
      Map<String, PermissionSet> allPermissions,
      String tableName,
      Predicate<PermissionSet.TablePermissions> scopeCheck) {
    for (PermissionSet perms : allPermissions.values()) {
      PermissionSet.TablePermissions tablePerms = perms.getTables().get(tableName);
      if (tablePerms != null && scopeCheck.test(tablePerms)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasOwnScope(PermissionSet.TablePermissions tp) {
    return tp.getSelect() == SelectScope.OWN
        || tp.getInsert() == SelectScope.OWN
        || tp.getUpdate() == SelectScope.OWN
        || tp.getDelete() == SelectScope.OWN;
  }

  private boolean hasGroupScope(PermissionSet.TablePermissions tp) {
    return tp.getSelect() == SelectScope.GROUP
        || tp.getInsert() == SelectScope.GROUP
        || tp.getUpdate() == SelectScope.GROUP
        || tp.getDelete() == SelectScope.GROUP;
  }

  private boolean columnExists(
      DSLContext txJooq, String schemaName, String tableName, String columnName) {
    return txJooq.fetchExists(
        txJooq
            .select()
            .from(name("information_schema", "columns"))
            .where(
                field(name("table_schema"))
                    .eq(inline(schemaName))
                    .and(field(name("table_name")).eq(inline(tableName)))
                    .and(field(name("column_name")).eq(inline(columnName)))));
  }

  private void addOwnerColumn(DSLContext txJooq, String schemaName, String tableName) {
    txJooq.execute(
        "ALTER TABLE {0} ADD COLUMN IF NOT EXISTS {1} TEXT DEFAULT current_user",
        table(name(schemaName, tableName)), name(MG_OWNER_COLUMN));
    txJooq.execute(
        "CREATE INDEX IF NOT EXISTS {0} ON {1} ({2})",
        name(tableName + "_" + MG_OWNER_COLUMN + "_btree"),
        table(name(schemaName, tableName)),
        name(MG_OWNER_COLUMN));
  }

  private void addGroupsColumn(DSLContext txJooq, String schemaName, String tableName) {
    txJooq.execute(
        "ALTER TABLE {0} ADD COLUMN IF NOT EXISTS {1} TEXT[]",
        table(name(schemaName, tableName)), name(MG_GROUPS_COLUMN));
    txJooq.execute(
        "CREATE INDEX IF NOT EXISTS {0} ON {1} USING GIN ({2})",
        name(tableName + "_" + MG_GROUPS_COLUMN + "_gin"),
        table(name(schemaName, tableName)),
        name(MG_GROUPS_COLUMN));
  }

  private void dropColumn(
      DSLContext txJooq, String schemaName, String tableName, String columnName) {
    txJooq.execute(
        "ALTER TABLE {0} DROP COLUMN IF EXISTS {1} CASCADE",
        table(name(schemaName, tableName)), name(columnName));
  }

  public PermissionSet getPermissions(Schema schema, String roleName) {
    String schemaName = schema.getName();
    String fullRole = fullRoleName(schemaName, roleName);
    if (!jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullRole))))) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    Record commentRecord =
        jooq()
            .fetchOne(
                "SELECT d.description FROM pg_authid a "
                    + "LEFT JOIN pg_shdescription d ON d.objoid = a.oid AND d.classoid = 'pg_authid'::regclass "
                    + "WHERE a.rolname = {0}",
                inline(fullRole));
    String comment = commentRecord == null ? null : commentRecord.get("description", String.class);
    if (comment == null || comment.isBlank() || comment.equals("{}")) {
      return new PermissionSet();
    }
    return deserializePermissionSet(comment);
  }

  private static String serializePermissionSet(PermissionSet permissions) {
    try {
      ObjectNode root = OBJECT_MAPPER.createObjectNode();
      ObjectNode tablesNode = root.putObject("tables");
      for (Map.Entry<String, PermissionSet.TablePermissions> entry :
          permissions.getTables().entrySet()) {
        PermissionSet.TablePermissions tp = entry.getValue();
        ObjectNode tableNode = tablesNode.putObject(entry.getKey());
        tableNode.put("select", tp.getSelect().name());
        tableNode.put("insert", tp.getInsert().name());
        tableNode.put("update", tp.getUpdate().name());
        tableNode.put("delete", tp.getDelete().name());
      }
      root.put("changeOwner", permissions.isChangeOwner());
      root.put("changeGroup", permissions.isChangeGroup());
      return OBJECT_MAPPER.writeValueAsString(root);
    } catch (Exception e) {
      throw new MolgenisException("Failed to serialize PermissionSet", e);
    }
  }

  private static PermissionSet deserializePermissionSet(String json) {
    try {
      ObjectNode root = (ObjectNode) OBJECT_MAPPER.readTree(json);
      PermissionSet result = new PermissionSet();
      if (root.has("tables")) {
        ObjectNode tablesNode = (ObjectNode) root.get("tables");
        tablesNode
            .fields()
            .forEachRemaining(
                entry -> {
                  ObjectNode tableNode = (ObjectNode) entry.getValue();
                  PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
                  if (tableNode.has("select")) {
                    tp.setSelect(SelectScope.fromString(tableNode.get("select").asText()));
                  }
                  if (tableNode.has("insert")) {
                    tp.setInsert(SelectScope.fromString(tableNode.get("insert").asText()));
                  }
                  if (tableNode.has("update")) {
                    tp.setUpdate(SelectScope.fromString(tableNode.get("update").asText()));
                  }
                  if (tableNode.has("delete")) {
                    tp.setDelete(SelectScope.fromString(tableNode.get("delete").asText()));
                  }
                  result.putTable(entry.getKey(), tp);
                });
      }
      if (root.has("changeOwner")) {
        result.setChangeOwner(root.get("changeOwner").asBoolean());
      }
      if (root.has("changeGroup")) {
        result.setChangeGroup(root.get("changeGroup").asBoolean());
      }
      return result;
    } catch (MolgenisException e) {
      throw e;
    } catch (Exception e) {
      throw new MolgenisException("Failed to deserialize PermissionSet", e);
    }
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
