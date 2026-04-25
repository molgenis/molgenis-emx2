package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.*;
import org.molgenis.emx2.TablePermission.Scope;
import org.molgenis.emx2.TablePermission.Select;
import org.molgenis.emx2.sql.rls.SqlPermissionExecutor;

public class SqlRoleManager implements RoleManager {

  private static final String GLOBAL_ROLE_PG_PREFIX = "MG_ROLE_";
  public static final String PG_ROLES = "pg_roles";
  public static final String ROLNAME = "rolname";
  public static final int PG_MAX_ID_LENGTH = 63;
  private static final String ROLE_NAME_PATTERN = "^[a-zA-Z0-9]([a-zA-Z0-9 ]*[a-zA-Z0-9])?$";

  private static final Set<String> IMMUTABLE_ROLE_BASENAMES =
      Set.of("viewer", "editor", "manager", "owner", "aggregator", "range", "exists", "count");

  private static final String ROLE_NAME_EMPTY = "Role name must not be empty";
  private static final String ROLE_NAME_INVALID_CHARS =
      "contains invalid characters; allowed: a-zA-Z0-9 and space, no leading/trailing space";
  private static final String ROLE_ALREADY_EXISTS = "role name already exists: ";
  private static final String ROLE_NOT_FOUND = "role not found: ";
  private static final String ROLE_IS_IMMUTABLE = "role is immutable (built-in): ";
  private static final String ADMIN_ONLY = "admin only";
  private static final String BUILTIN_COLLISION =
      "role name collides with a built-in privilege name: ";

  private final SqlDatabase database;

  public SqlRoleManager(SqlDatabase database) {
    this.database = database;
  }

  private DSLContext jooq() {
    return database.getJooq();
  }

  private static void validateRoleName(String name) {
    if (name == null || name.isEmpty()) {
      throw new MolgenisException(ROLE_NAME_EMPTY);
    }
    if (name.length() > MAX_ROLE_NAME_LENGTH) {
      throw new MolgenisException(
          "Role name '"
              + name
              + "' exceeds maximum length of "
              + MAX_ROLE_NAME_LENGTH
              + " characters");
    }
    if (!name.matches(ROLE_NAME_PATTERN)) {
      throw new MolgenisException("Role name '" + name + "' " + ROLE_NAME_INVALID_CHARS);
    }
  }

  @Override
  public void createRole(String name, String description) {
    if (!database.isAdmin()) {
      throw new MolgenisException(ADMIN_ONLY);
    }
    validateRoleName(name);
    if (isBuiltinCollision(name)) {
      throw new MolgenisException(BUILTIN_COLLISION + name);
    }
    if (pgRoleExists(name)) {
      throw new MolgenisException(ROLE_ALREADY_EXISTS + name);
    }
    String pgRoleName = pgRoleName(name);
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            jooq.execute("CREATE ROLE {0} NOLOGIN", name(pgRoleName));
            if (description != null) {
              jooq.execute("COMMENT ON ROLE {0} IS {1}", name(pgRoleName), inline(description));
            }
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  @Override
  public void deleteRole(String name) {
    if (!database.isAdmin()) {
      throw new MolgenisException(ADMIN_ONLY);
    }
    if (isSystemRoleByName(name)) {
      throw new MolgenisException(ROLE_IS_IMMUTABLE + name);
    }
    if (!pgRoleExists(name)) {
      throw new MolgenisException(ROLE_NOT_FOUND + name);
    }
    String pgRoleName = pgRoleName(name);
    String policyPattern = "MG_P_" + name + "_%";
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();

            dropPoliciesForRole(jooq, policyPattern);
            revokeAllGrantsForRole(jooq, pgRoleName);

            jooq.execute("REASSIGN OWNED BY {0} TO {1}", name(pgRoleName), name("MG_USER_admin"));
            jooq.execute("DROP OWNED BY {0}", name(pgRoleName));
            jooq.execute("DROP ROLE IF EXISTS {0}", name(pgRoleName));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  private void dropPoliciesForRole(DSLContext jooq, String policyPattern) {
    List<Record> policies =
        jooq.fetch(
            "SELECT schemaname, tablename, policyname FROM pg_policies WHERE policyname LIKE {0}",
            inline(policyPattern));
    for (Record policy : policies) {
      String schemaName = policy.get("schemaname", String.class);
      String tableName = policy.get("tablename", String.class);
      String policyName = policy.get("policyname", String.class);
      jooq.execute(
          "DROP POLICY IF EXISTS {0} ON {1}", name(policyName), table(name(schemaName, tableName)));
    }
  }

  private void revokeAllGrantsForRole(DSLContext jooq, String pgRoleName) {
    List<Record> grants =
        jooq.fetch(
            "SELECT table_schema, table_name, privilege_type FROM information_schema.role_table_grants WHERE grantee = {0}",
            inline(pgRoleName));
    for (Record grant : grants) {
      String schemaName = grant.get("table_schema", String.class);
      String tableName = grant.get("table_name", String.class);
      String privilegeType = grant.get("privilege_type", String.class);
      jooq.execute(
          "REVOKE {0} ON {1} FROM {2}",
          keyword(privilegeType), table(name(schemaName, tableName)), name(pgRoleName));
    }
  }

  @Override
  public List<Role> listRoles() {
    if (!database.isAdmin()) {
      throw new MolgenisException(ADMIN_ONLY);
    }
    return jooq()
        .fetch(
            "SELECT a.rolname, d.description "
                + "FROM pg_authid a "
                + "LEFT JOIN pg_shdescription d ON d.objoid = a.oid AND d.classoid = 'pg_authid'::regclass "
                + "WHERE a.rolname LIKE {0} "
                + "ORDER BY a.rolname",
            inline(GLOBAL_ROLE_PG_PREFIX + "%"))
        .stream()
        .map(
            r -> {
              String pgName = r.get("rolname", String.class);
              String baseName = pgName.substring(GLOBAL_ROLE_PG_PREFIX.length());
              return new Role(baseName)
                  .setDescription(r.get("description", String.class))
                  .setSystemRole(isSystemRoleByName(baseName));
            })
        .toList();
  }

  @Override
  public void grantRoleToUser(String role, String user) {
    if (!database.isAdmin()) {
      throw new MolgenisException(ADMIN_ONLY);
    }
    String pgRole = GLOBAL_ROLE_PG_PREFIX + role;
    String pgUser = MG_USER_PREFIX + user;
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            ((SqlDatabase) db).getJooq().execute("GRANT {0} TO {1}", name(pgRole), name(pgUser));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  @Override
  public void revokeRoleFromUser(String role, String user) {
    if (!database.isAdmin()) {
      throw new MolgenisException(ADMIN_ONLY);
    }
    String pgRole = GLOBAL_ROLE_PG_PREFIX + role;
    String pgUser = MG_USER_PREFIX + user;
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            ((SqlDatabase) db).getJooq().execute("REVOKE {0} FROM {1}", name(pgRole), name(pgUser));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  @Override
  public void setPermissions(String role, PermissionSet permissions) {
    permissions.validateOrThrow();
    String pgRole = GLOBAL_ROLE_PG_PREFIX + role;
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();

            Map<String, TablePermission> oldMap =
                permissionsByKey(getPermissionsNoAdminCheck(role));
            Map<String, TablePermission> newMap = materialiseAll(jooq, permissions);

            applyDiff(jooq, pgRole, oldMap, newMap);
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  private Map<String, TablePermission> permissionsByKey(PermissionSet ps) {
    Map<String, TablePermission> map = new LinkedHashMap<>();
    for (TablePermission p : ps) {
      map.put(p.schema() + ":" + p.table(), p);
    }
    return map;
  }

  private Map<String, TablePermission> materialiseAll(DSLContext jooq, PermissionSet permissions) {
    Map<String, TablePermission> map = new LinkedHashMap<>();
    for (TablePermission p : permissions) {
      if (isWildcard(p)) {
        for (TablePermission m : materialiseWildcard(jooq, p)) {
          map.put(m.schema() + ":" + m.table(), m);
        }
      } else {
        map.put(p.schema() + ":" + p.table(), p);
      }
    }
    return map;
  }

  private void applyDiff(
      DSLContext jooq,
      String pgRole,
      Map<String, TablePermission> oldMap,
      Map<String, TablePermission> newMap) {
    Set<String> oldSchemas = schemasOf(oldMap);
    Set<String> newSchemas = schemasOf(newMap);

    for (String key : oldMap.keySet()) {
      if (!newMap.containsKey(key)) {
        TablePermission old = oldMap.get(key);
        SqlPermissionExecutor.revokeAllTablePrivileges(jooq, pgRole, old.schema(), old.table());
      }
    }

    List<String> schemasGranted = new ArrayList<>();
    for (String key : newMap.keySet()) {
      TablePermission newPerm = newMap.get(key);
      TablePermission oldPerm = oldMap.get(key);
      if (oldPerm == null) {
        emitPermission(jooq, pgRole, newPerm);
        emitFlagPolicies(jooq, pgRole, newPerm);
        grantSchemaAccessIfNeeded(jooq, pgRole, newPerm.schema(), schemasGranted);
      } else if (!oldPerm.equals(newPerm)) {
        SqlPermissionExecutor.revokeAllTablePrivileges(
            jooq, pgRole, oldPerm.schema(), oldPerm.table());
        emitPermission(jooq, pgRole, newPerm);
        emitFlagPolicies(jooq, pgRole, newPerm);
        grantSchemaAccessIfNeeded(jooq, pgRole, newPerm.schema(), schemasGranted);
      }
    }

    for (String schema : oldSchemas) {
      if (!newSchemas.contains(schema)) {
        revokeSchemaAccessForSchema(jooq, pgRole, schema);
      }
    }
  }

  private Set<String> schemasOf(Map<String, TablePermission> map) {
    Set<String> schemas = new LinkedHashSet<>();
    for (TablePermission p : map.values()) {
      schemas.add(p.schema());
    }
    return schemas;
  }

  private void grantSchemaAccessIfNeeded(
      DSLContext jooq, String pgRole, String schemaName, List<String> schemasGranted) {
    if (schemasGranted.contains(schemaName)) return;
    schemasGranted.add(schemaName);
    String existsRole = fullRoleName(schemaName, Privileges.EXISTS.toString());
    jooq.execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schemaName), name(pgRole));
    boolean existsRolePresent =
        jooq.fetchExists(
            jooq.select()
                .from(PG_ROLES)
                .where(org.jooq.impl.DSL.field(ROLNAME).eq(org.jooq.impl.DSL.inline(existsRole))));
    if (existsRolePresent) {
      jooq.execute("GRANT {0} TO {1}", name(existsRole), name(pgRole));
    }
  }

  private void revokeSchemaAccessForSchema(DSLContext jooq, String pgRole, String schemaName) {
    String existsRole = fullRoleName(schemaName, Privileges.EXISTS.toString());
    boolean existsRoleExists =
        jooq.fetchExists(
            jooq.select()
                .from(PG_ROLES)
                .where(org.jooq.impl.DSL.field(ROLNAME).eq(org.jooq.impl.DSL.inline(existsRole))));
    if (existsRoleExists) {
      jooq.execute("REVOKE {0} FROM {1}", name(existsRole), name(pgRole));
    }
    jooq.execute("REVOKE USAGE ON SCHEMA {0} FROM {1}", name(schemaName), name(pgRole));
  }

  private static boolean isWildcard(TablePermission p) {
    return p.schema().equals("*") || p.table().equals("*");
  }

  private List<TablePermission> materialiseWildcard(DSLContext jooq, TablePermission p) {
    List<TablePermission> result = new ArrayList<>();
    boolean schemaWild = p.schema().equals("*");
    boolean tableWild = p.table().equals("*");

    if (schemaWild && tableWild) {
      List<String> schemas = listUserSchemas(jooq);
      for (String schema : schemas) {
        for (String table : listTablesInSchema(jooq, schema)) {
          result.add(concretePermission(p, schema, table));
        }
      }
    } else if (schemaWild) {
      List<String> schemas = listUserSchemas(jooq);
      for (String schema : schemas) {
        List<String> tables = listTablesInSchema(jooq, schema);
        if (tables.contains(p.table())) {
          result.add(concretePermission(p, schema, p.table()));
        }
      }
    } else {
      for (String table : listTablesInSchema(jooq, p.schema())) {
        result.add(concretePermission(p, p.schema(), table));
      }
    }
    return result;
  }

  private static TablePermission concretePermission(
      TablePermission template, String schema, String table) {
    return new TablePermission(
        schema,
        table,
        template.select(),
        template.insert(),
        template.update(),
        template.delete(),
        template.changeOwner(),
        template.share());
  }

  private List<String> listUserSchemas(DSLContext jooq) {
    return jooq.fetch(
            "SELECT schema_name FROM information_schema.schemata WHERE schema_name NOT IN ('MOLGENIS', 'pg_catalog', 'information_schema', 'pg_toast') AND schema_name NOT LIKE 'pg_%' AND LEFT(schema_name, 1) != '_'")
        .map(r -> r.get("schema_name", String.class));
  }

  private List<String> listTablesInSchema(DSLContext jooq, String schema) {
    return jooq.fetch(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = {0} AND table_type = 'BASE TABLE'",
            inline(schema))
        .map(r -> r.get("table_name", String.class));
  }

  private static String role(String pgRole) {
    if (pgRole.startsWith(GLOBAL_ROLE_PG_PREFIX)) {
      return pgRole.substring(GLOBAL_ROLE_PG_PREFIX.length());
    }
    return pgRole;
  }

  private static void emitPermission(DSLContext jooq, String pgRole, TablePermission p) {
    emitSelectVerb(jooq, pgRole, p.schema(), p.table(), p.select());
    emitVerb(jooq, pgRole, p.schema(), p.table(), SQL_INSERT, p.insert());
    emitVerb(jooq, pgRole, p.schema(), p.table(), SQL_UPDATE, p.update());
    emitVerb(jooq, pgRole, p.schema(), p.table(), SQL_DELETE, p.delete());
    ensureAllScopePoliciesEmittedIfRlsInstalled(jooq, pgRole, p);
  }

  private static void emitSelectVerb(
      DSLContext jooq, String pgRole, String schema, String table, Select select) {
    if (select == Select.NONE) return;
    SqlPermissionExecutor.grantTablePrivilege(jooq, pgRole, schema, table, SQL_SELECT);
    if (select == Select.OWN) {
      SqlPermissionExecutor.ensureRlsInstalled(jooq, schema, table);
      SqlPermissionExecutor.createPolicy(jooq, pgRole, schema, table, SQL_SELECT, Scope.OWN);
    } else if (select == Select.GROUP) {
      SqlPermissionExecutor.ensureRlsInstalled(jooq, schema, table);
      SqlPermissionExecutor.createPolicy(jooq, pgRole, schema, table, SQL_SELECT, Scope.GROUP);
    } else if (select == Select.ALL) {
      SqlPermissionExecutor.createPolicy(jooq, pgRole, schema, table, SQL_SELECT, Scope.ALL);
    } else {
      SqlPermissionExecutor.ensureRlsInstalled(jooq, schema, table);
      SqlPermissionExecutor.createSelectModePolicy(jooq, pgRole, schema, table, select);
    }
  }

  private static void ensureAllScopePoliciesEmittedIfRlsInstalled(
      DSLContext jooq, String pgRole, TablePermission p) {
    if (!SqlPermissionExecutor.isRlsEnabled(jooq, p.schema(), p.table())) return;
    String rawRole = role(pgRole);
    if (p.select() == Select.ALL) {
      createPolicyIfAbsent(jooq, pgRole, rawRole, p.schema(), p.table(), SQL_SELECT, Scope.ALL);
    }
    if (p.insert() == Scope.ALL) {
      createPolicyIfAbsent(jooq, pgRole, rawRole, p.schema(), p.table(), SQL_INSERT, Scope.ALL);
    }
    if (p.update() == Scope.ALL) {
      createPolicyIfAbsent(jooq, pgRole, rawRole, p.schema(), p.table(), SQL_UPDATE, Scope.ALL);
    }
    if (p.delete() == Scope.ALL) {
      createPolicyIfAbsent(jooq, pgRole, rawRole, p.schema(), p.table(), SQL_DELETE, Scope.ALL);
    }
  }

  private static void createPolicyIfAbsent(
      DSLContext jooq,
      String pgRole,
      String rawRole,
      String schema,
      String table,
      String verb,
      Scope scope) {
    String policyName = "MG_P_" + rawRole + "_" + verb + "_" + scope.name();
    boolean exists =
        jooq.fetchExists(
            jooq.select()
                .from("pg_policies")
                .where(
                    org.jooq
                        .impl
                        .DSL
                        .field("schemaname")
                        .eq(org.jooq.impl.DSL.inline(schema))
                        .and(
                            org.jooq
                                .impl
                                .DSL
                                .field("tablename")
                                .eq(org.jooq.impl.DSL.inline(table)))
                        .and(
                            org.jooq
                                .impl
                                .DSL
                                .field("policyname")
                                .eq(org.jooq.impl.DSL.inline(policyName)))));
    if (!exists) {
      SqlPermissionExecutor.createPolicy(jooq, pgRole, schema, table, verb, scope);
    }
  }

  private static void emitVerb(
      DSLContext jooq, String pgRole, String schema, String table, String verb, Scope scope) {
    if (scope == Scope.NONE) return;
    SqlPermissionExecutor.grantTablePrivilege(jooq, pgRole, schema, table, verb);
    if (scope == Scope.OWN || scope == Scope.GROUP) {
      SqlPermissionExecutor.ensureRlsInstalled(jooq, schema, table);
      SqlPermissionExecutor.createPolicy(jooq, pgRole, schema, table, verb, scope);
    } else if (scope == Scope.ALL && SqlPermissionExecutor.isRlsEnabled(jooq, schema, table)) {
      SqlPermissionExecutor.createPolicy(jooq, pgRole, schema, table, verb, Scope.ALL);
    }
  }

  private static void emitFlagPolicies(DSLContext jooq, String pgRole, TablePermission p) {
    Scope updateScope = p.update();
    if ((p.changeOwner() || p.share()) && updateScope != Scope.NONE) {
      SqlPermissionExecutor.ensureRlsInstalled(jooq, p.schema(), p.table());
      if (p.changeOwner()) {
        SqlPermissionExecutor.createChangeOwnerPolicy(
            jooq, pgRole, p.schema(), p.table(), updateScope);
      }
      if (p.share()) {
        SqlPermissionExecutor.createSharePolicy(jooq, pgRole, p.schema(), p.table(), updateScope);
      }
    }
  }

  @Override
  public PermissionSet getPermissions(String role) {
    if (!database.isAdmin()) {
      throw new MolgenisException(ADMIN_ONLY);
    }
    return getPermissionsNoAdminCheck(role);
  }

  private PermissionSet getPermissionsNoAdminCheck(String role) {
    String pgRole = GLOBAL_ROLE_PG_PREFIX + role;
    return SqlPermissionExecutor.readPolicies(jooq(), pgRole);
  }

  @Override
  public PermissionSet getPermissionsForActiveUser() {
    String activeUser = database.getActiveUser();
    if (activeUser == null) {
      return new PermissionSet();
    }
    String pgUser = Constants.MG_USER_PREFIX + activeUser;
    List<String> roles =
        jooq()
            .fetch(
                "SELECT r.rolname FROM pg_catalog.pg_auth_members am "
                    + "JOIN pg_catalog.pg_roles r ON (r.oid = am.roleid) "
                    + "JOIN pg_catalog.pg_roles m ON (m.oid = am.member) "
                    + "WHERE m.rolname = {0} AND r.rolname LIKE {1} AND r.rolname NOT LIKE {2}",
                org.jooq.impl.DSL.inline(pgUser),
                org.jooq.impl.DSL.inline(GLOBAL_ROLE_PG_PREFIX + "%"),
                org.jooq.impl.DSL.inline(GLOBAL_ROLE_PG_PREFIX + "%/%"))
            .map(r -> r.get("rolname", String.class).substring(GLOBAL_ROLE_PG_PREFIX.length()));

    Map<String, TablePermission> byKey = new LinkedHashMap<>();
    for (String role : roles) {
      for (TablePermission p : getPermissionsNoAdminCheck(role)) {
        String key = p.schema() + ":" + p.table();
        TablePermission existing = byKey.get(key);
        if (existing == null) {
          byKey.put(key, p);
        } else {
          byKey.put(
              key,
              new TablePermission(
                  p.schema(),
                  p.table(),
                  maxSelect(existing.select(), p.select()),
                  maxScope(existing.insert(), p.insert()),
                  maxScope(existing.update(), p.update()),
                  maxScope(existing.delete(), p.delete()),
                  existing.changeOwner() || p.changeOwner(),
                  existing.share() || p.share()));
        }
      }
    }
    PermissionSet merged = new PermissionSet();
    for (TablePermission p : byKey.values()) {
      merged.put(p);
    }
    return merged;
  }

  private static Select maxSelect(Select a, Select b) {
    return a.permissivenessLevel() >= b.permissivenessLevel() ? a : b;
  }

  private static Scope maxScope(Scope a, Scope b) {
    return a.ordinal() >= b.ordinal() ? a : b;
  }

  public void createOrUpdateRole(String name, String description) {
    if (pgRoleExists(name)) {
      updateRoleDescription(name, description);
    } else {
      createRole(name, description);
    }
  }

  public void updateRoleDescription(String name, String description) {
    if (!pgRoleExists(name)) {
      throw new MolgenisException(ROLE_NOT_FOUND + name);
    }
    String pgRoleName = pgRoleName(name);
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            if (description != null) {
              jooq.execute("COMMENT ON ROLE {0} IS {1}", name(pgRoleName), inline(description));
            } else {
              jooq.execute("COMMENT ON ROLE {0} IS NULL", name(pgRoleName));
            }
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  public List<String> getMembersForRole(String role) {
    String pgRole = GLOBAL_ROLE_PG_PREFIX + role;
    return jooq()
        .fetch(
            "SELECT m.rolname FROM pg_catalog.pg_auth_members am "
                + "JOIN pg_catalog.pg_roles m ON (m.oid = am.member) "
                + "JOIN pg_catalog.pg_roles r ON (r.oid = am.roleid) "
                + "WHERE r.rolname = {0} AND m.rolname LIKE {1}",
            org.jooq.impl.DSL.inline(pgRole),
            org.jooq.impl.DSL.inline(Constants.MG_USER_PREFIX + "%"))
        .map(r -> r.get("rolname", String.class).substring(Constants.MG_USER_PREFIX.length()));
  }

  private boolean isBuiltinCollision(String name) {
    return Arrays.stream(Privileges.values()).anyMatch(p -> p.toString().equalsIgnoreCase(name));
  }

  private boolean isSystemRoleByName(String name) {
    return IMMUTABLE_ROLE_BASENAMES.contains(name.toLowerCase());
  }

  private String pgRoleName(String baseName) {
    return GLOBAL_ROLE_PG_PREFIX + baseName;
  }

  private boolean pgRoleExists(String baseName) {
    return jooq()
        .fetchExists(
            jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(pgRoleName(baseName)))));
  }

  public void createSchemaRole(String schemaName, String roleName) {
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

  public void deleteSchemaRole(String schemaName, String roleName) {
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
    if (p.select() != Select.NONE) {
      emitSelectVerb(jooq(), fullRole, schemaName, tableName, p.select());
      ensureAllScopePoliciesEmittedIfRlsInstalled(
          jooq(),
          fullRole,
          new TablePermission(
              schemaName, tableName, p.select(), Scope.NONE, Scope.NONE, Scope.NONE, false, false));
    } else {
      jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.insert() != Scope.NONE) {
      jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
    } else {
      jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.update() != Scope.NONE) {
      jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
    } else {
      jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.delete() != Scope.NONE) {
      jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
    } else {
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
      Map<String, Select> selectModeByTable = readSelectModeFromPolicies(fullRole, schemaName);
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
        Select select;
        if (!Boolean.TRUE.equals(row.get("can_select", Boolean.class))) {
          select = Select.NONE;
        } else {
          select = selectModeByTable.getOrDefault(tableName, Select.ALL);
        }
        Scope insert =
            Boolean.TRUE.equals(row.get("can_insert", Boolean.class)) ? Scope.ALL : Scope.NONE;
        Scope update =
            Boolean.TRUE.equals(row.get("can_update", Boolean.class)) ? Scope.ALL : Scope.NONE;
        Scope delete =
            Boolean.TRUE.equals(row.get("can_delete", Boolean.class)) ? Scope.ALL : Scope.NONE;
        result.add(
            new TablePermission(
                schemaName, tableName, select, insert, update, delete, false, false));
      }
    } catch (Exception e) {
      throw new SqlMolgenisException("Failed to get permissions for " + roleName, e);
    }
    return result;
  }

  private Map<String, Select> readSelectModeFromPolicies(String fullRole, String schemaName) {
    String rawRole =
        fullRole.startsWith(MG_ROLE_PREFIX)
            ? fullRole.substring(MG_ROLE_PREFIX.length())
            : fullRole;
    String selectPrefix = "MG_P_" + rawRole + "_SELECT_";
    String policyPattern = selectPrefix + "%";
    Map<String, Select> result = new LinkedHashMap<>();
    jooq()
        .fetch(
            "SELECT tablename, policyname FROM pg_policies WHERE schemaname = {0} AND policyname LIKE {1}",
            inline(schemaName), inline(policyPattern))
        .forEach(
            row -> {
              String tableName = row.get("tablename", String.class);
              String policyName = row.get("policyname", String.class);
              String selectName = policyName.substring(selectPrefix.length());
              try {
                Select parsed = Select.fromString(selectName);
                if (parsed != Select.NONE) {
                  result.merge(
                      tableName,
                      parsed,
                      (a, b) -> a.permissivenessLevel() >= b.permissivenessLevel() ? a : b);
                }
              } catch (MolgenisException ignored) {
                // unknown select mode in policy name — skip
              }
            });
    return result;
  }

  public Role getRole(String schemaName, String roleName) {
    return new Role(roleName)
        .setSystemRole(isSystemRole(roleName))
        .withPermissions(getPermissions(schemaName, roleName));
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

    Map<String, TablePermission> merged = new LinkedHashMap<>();
    for (String roleName : roleNames) {
      for (TablePermission p : getPermissions(schemaName, roleName)) {
        if (hasAnyPermission(p)) {
          merged.merge(p.table(), p, SqlRoleManager::mergePermissions);
        }
      }
    }
    for (TablePermission p : getPermissionsForActiveUser()) {
      if (schemaName.equals(p.schema()) && hasAnyPermission(p)) {
        merged.merge(p.table(), p, SqlRoleManager::mergePermissions);
      }
    }
    expandWildcard(merged, schemaName, schema.getTableNames());
    return new ArrayList<>(merged.values());
  }

  private static void expandWildcard(
      Map<String, TablePermission> permissions, String schemaName, Collection<String> tableNames) {
    TablePermission wildcard = permissions.remove("*");
    if (wildcard == null) return;
    for (String tableName : tableNames) {
      permissions.merge(
          tableName,
          new TablePermission(
              schemaName,
              tableName,
              wildcard.select(),
              wildcard.insert(),
              wildcard.update(),
              wildcard.delete(),
              wildcard.changeOwner(),
              wildcard.share()),
          SqlRoleManager::mergePermissions);
    }
  }

  private static boolean hasAnyPermission(TablePermission p) {
    return p.select() != Select.NONE
        || p.insert() != Scope.NONE
        || p.update() != Scope.NONE
        || p.delete() != Scope.NONE;
  }

  private static TablePermission mergePermissions(TablePermission a, TablePermission b) {
    return new TablePermission(
        a.schema(),
        a.table(),
        maxSelect(a.select(), b.select()),
        maxScope(a.insert(), b.insert()),
        maxScope(a.update(), b.update()),
        maxScope(a.delete(), b.delete()),
        a.changeOwner() || b.changeOwner(),
        a.share() || b.share());
  }

  public boolean isSystemRole(String roleName) {
    return Privileges.isSystemRole(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  private List<TablePermission> systemPermissions(String roleName) {
    if (roleName.equals(Privileges.EXISTS.toString())) {
      return List.of(
          new TablePermission(
              "*", "*", Select.EXISTS, Scope.NONE, Scope.NONE, Scope.NONE, false, false));
    } else if (roleName.equals(Privileges.RANGE.toString())) {
      return List.of(
          new TablePermission(
              "*", "*", Select.RANGE, Scope.NONE, Scope.NONE, Scope.NONE, false, false));
    } else if (roleName.equals(Privileges.AGGREGATOR.toString())) {
      return List.of(
          new TablePermission(
              "*", "*", Select.AGGREGATE, Scope.NONE, Scope.NONE, Scope.NONE, false, false));
    } else if (roleName.equals(Privileges.COUNT.toString())) {
      return List.of(
          new TablePermission(
              "*", "*", Select.COUNT, Scope.NONE, Scope.NONE, Scope.NONE, false, false));
    } else if (roleName.equals(Privileges.VIEWER.toString())) {
      return List.of(
          new TablePermission(
              "*", "*", Select.ALL, Scope.NONE, Scope.NONE, Scope.NONE, false, false));
    } else if (roleName.equals(Privileges.EDITOR.toString())
        || roleName.equals(Privileges.MANAGER.toString())
        || roleName.equals(Privileges.OWNER.toString())) {
      return List.of(
          new TablePermission("*", "*", Select.ALL, Scope.ALL, Scope.ALL, Scope.ALL, false, false));
    }
    return List.of();
  }
}
