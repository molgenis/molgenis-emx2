package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlRoleManager {

  private static final Logger logger = LoggerFactory.getLogger(SqlRoleManager.class);

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

  public boolean isSystemRole(String roleName) {
    return Privileges.isSystemRole(roleName);
  }

  private void requireManagerOrOwner(Schema schema) {
    if (database.isAdmin()) return;
    if (schema.hasActiveUserRole(Privileges.MANAGER)) return;
    if (schema.hasActiveUserRole(Privileges.OWNER)) return;
    throw new MolgenisException(
        "Only admin, Owner or Manager can manage roles, groups, and memberships on schema "
            + schema.getName());
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  public void createRole(String schemaName, String roleName) {
    Schema schema = database.getSchema(schemaName);
    if (schema != null) requireManagerOrOwner(schema);
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot create system role: " + roleName);
    }
    if (!roleName.matches(Constants.ROLE_NAME_REGEX)) {
      throw new MolgenisException("Invalid role name '" + roleName + "'");
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
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext adminJooq = ((SqlDatabase) db).getJooq();
            executeCreateRole(adminJooq, fullRole);
            adminJooq.execute("GRANT {0} TO session_user WITH ADMIN OPTION", name(fullRole));
            adminJooq.execute(
                "GRANT {0} TO {1} WITH ADMIN OPTION", name(fullRole), name(ownerRole));
            adminJooq.execute("GRANT {0} TO {1}", name(existsRole), name(fullRole));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
    database.getListener().onSchemaChange();
  }

  public void createRole(Schema schema, String roleName, String description) {
    requireManagerOrOwner(schema);
    createRole(schema.getName(), roleName);
  }

  public void deleteRole(String schemaName, String roleName) {
    Schema schema = database.getSchema(schemaName);
    if (schema != null) requireManagerOrOwner(schema);
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot delete system role: " + roleName);
    }
    if (!roleExists(schemaName, roleName)) {
      throw new MolgenisException("Role does not exist: " + roleName);
    }
    String fullRole = fullRoleName(schemaName, roleName);
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext adminJooq = ((SqlDatabase) db).getJooq();
            for (String tableName : database.getSchema(schemaName).getTableNames()) {
              adminJooq.execute(
                  "REVOKE ALL ON {0} FROM {1}", table(name(schemaName, tableName)), name(fullRole));
            }
            adminJooq.execute(
                """
                        DO $$ DECLARE m TEXT; BEGIN
                         FOR m IN SELECT rolname FROM pg_roles
                         WHERE pg_has_role(rolname, {0}, 'member') AND rolname <> {0}
                         LOOP EXECUTE 'REVOKE ' || quote_ident({0}) || ' FROM ' || quote_ident(m);
                         END LOOP; END $$;""",
                inline(fullRole));
            adminJooq.execute("DROP ROLE IF EXISTS {0}", name(fullRole));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
    database.getJooqAsAdmin(adminJooq -> deleteAllRolePermissions(adminJooq, schemaName, roleName));
    database.getJooqAsAdmin(
        adminJooq -> deleteAllGroupMembershipsForRole(adminJooq, schemaName, roleName));
    database.getListener().onSchemaChange();
  }

  public void deleteRole(Schema schema, String roleName) {
    requireManagerOrOwner(schema);
    deleteRole(schema.getName(), roleName);
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
    Schema schema = database.getSchema(schemaName);
    if (!schema.getTableNames().contains(tableName)) {
      throw new MolgenisException("Table does not exist: " + tableName);
    }
    TableMetadata tableMeta = schema.getMetadata().getTableMetadata(tableName);
    if (tableMeta != null) {
      tableMeta.rejectIfRlsScopeWithoutRls(permission, new PermissionSet());
    }
    TablePermission merged = mergeWithExisting(schemaName, roleName, tableName, permission);
    String fullRole = fullRoleName(schemaName, roleName);
    applyPgGrants(schemaName, fullRole, tableName, merged);
    database.getJooqAsAdmin(
        adminJooq ->
            upsertRolePermissionScopes(
                adminJooq,
                schemaName,
                roleName,
                tableName,
                selectScopeName(merged),
                updateScopeName(merged.insert()),
                updateScopeName(merged.update()),
                updateScopeName(merged.delete())));
    database.getListener().onSchemaChange();
  }

  private TablePermission mergeWithExisting(
      String schemaName, String roleName, String tableName, TablePermission incoming) {
    TablePermission existing = loadExistingGrant(schemaName, roleName, tableName);
    SelectScope mergedSelect =
        incoming.select() != SelectScope.NONE
            ? incoming.select()
            : (existing == null ? SelectScope.NONE : existing.select());
    UpdateScope mergedInsert =
        incoming.insert() != UpdateScope.NONE
            ? incoming.insert()
            : (existing == null ? UpdateScope.NONE : existing.insert());
    UpdateScope mergedUpdate =
        incoming.update() != UpdateScope.NONE
            ? incoming.update()
            : (existing == null ? UpdateScope.NONE : existing.update());
    UpdateScope mergedDelete =
        incoming.delete() != UpdateScope.NONE
            ? incoming.delete()
            : (existing == null ? UpdateScope.NONE : existing.delete());
    ReferenceScope mergedReference =
        incoming.reference() != ReferenceScope.NONE
            ? incoming.reference()
            : (existing == null ? ReferenceScope.NONE : existing.reference());
    return new TablePermission(tableName)
        .select(mergedSelect)
        .insert(mergedInsert)
        .update(mergedUpdate)
        .delete(mergedDelete)
        .reference(mergedReference);
  }

  private TablePermission loadExistingGrant(String schemaName, String roleName, String tableName) {
    TablePermission[] found = {null};
    database.getJooqAsAdmin(
        adminJooq -> {
          Record row = loadRolePermission(adminJooq, schemaName, roleName, tableName);
          if (row != null) {
            TablePermission tp =
                new TablePermission(tableName)
                    .select(SelectScope.fromString(row.get(RPM_SELECT_SCOPE)))
                    .insert(UpdateScope.fromString(row.get(RPM_INSERT_SCOPE)))
                    .update(UpdateScope.fromString(row.get(RPM_UPDATE_SCOPE)))
                    .delete(UpdateScope.fromString(row.get(RPM_DELETE_SCOPE)))
                    .reference(
                        PermissionSet.ReferenceScope.fromString(row.get(RPM_REFERENCE_SCOPE)));
            found[0] = tp;
          }
        });
    return found[0];
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
    database.getJooqAsAdmin(
        adminJooq -> deleteRolePermission(adminJooq, schemaName, roleName, tableName));
    database.getListener().onSchemaChange();
  }

  private void applyPgGrants(
      String schemaName, String fullRole, String tableName, TablePermission p) {
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    if (p.select() == SelectScope.ALL) {
      jooq().execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (p.select() == SelectScope.NONE) {
      jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.insert() == UpdateScope.ALL) {
      jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (p.insert() == UpdateScope.NONE) {
      jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.update() == UpdateScope.ALL) {
      jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (p.update() == UpdateScope.NONE) {
      jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.delete() == UpdateScope.ALL) {
      jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (p.delete() == UpdateScope.NONE) {
      jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
  }

  public void clearTablePermissionsForTable(String schemaName, String tableName) {
    database.getJooqAsAdmin(
        adminJooq -> deleteAllRolePermissionsForTable(adminJooq, schemaName, tableName));
  }

  public List<TablePermission> listTablePermissions(String schemaName, String roleName) {
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
        SelectScope selectScope =
            Boolean.TRUE.equals(row.get("can_select", Boolean.class))
                ? SelectScope.ALL
                : SelectScope.NONE;
        UpdateScope insertScope =
            Boolean.TRUE.equals(row.get("can_insert", Boolean.class))
                ? UpdateScope.ALL
                : UpdateScope.NONE;
        UpdateScope updateScope =
            Boolean.TRUE.equals(row.get("can_update", Boolean.class))
                ? UpdateScope.ALL
                : UpdateScope.NONE;
        UpdateScope deleteScope =
            Boolean.TRUE.equals(row.get("can_delete", Boolean.class))
                ? UpdateScope.ALL
                : UpdateScope.NONE;
        result.add(
            new TablePermission(tableName)
                .select(selectScope)
                .insert(insertScope)
                .update(updateScope)
                .delete(deleteScope));
      }
    } catch (Exception e) {
      throw new SqlMolgenisException("Failed to get permissions for " + roleName, e);
    }
    return result;
  }

  public Role getRole(String schemaName, String roleName) {
    boolean system = isSystemRole(roleName);
    if (system) {
      boolean canChangeOwnerAndGroup =
          roleName.equals(Privileges.MANAGER.toString())
              || roleName.equals(Privileges.OWNER.toString());
      return new Role(
          roleName,
          true,
          listTablePermissions(schemaName, roleName),
          "",
          canChangeOwnerAndGroup,
          canChangeOwnerAndGroup);
    }
    PermissionSet ps = getPermissionSet(schemaName, roleName);
    return new Role(
        roleName,
        false,
        listTablePermissions(schemaName, roleName),
        ps.getDescription(),
        ps.isChangeOwner(),
        ps.isChangeGroup());
  }

  public List<String> listRoles(String schemaName) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    return jooq()
        .select(field(ROLNAME))
        .from(PG_ROLES)
        .where(field(ROLNAME).like(inline(rolePrefix + "%")))
        .fetch(r -> r.get(ROLNAME, String.class).substring(rolePrefix.length()));
  }

  public List<String> listRoles(Schema schema) {
    return listRoles(schema.getName());
  }

  public List<Role> getRoles(String schemaName) {
    List<String> roleNames = listRoles(schemaName);
    List<Role> result = new ArrayList<>();
    for (String roleName : roleNames) {
      result.add(getRole(schemaName, roleName));
    }
    return result;
  }

  public List<TablePermission> getTablePermissionsForActiveUser(String schemaName) {
    String activeUser = database.getActiveUser();
    SqlSchema schema = database.getSchema(schemaName);
    if (schema == null) return List.of();
    List<String> roleNames = schema.getInheritedRolesForUser(activeUser);

    if (roleNames.isEmpty()) return List.of();

    Map<String, TablePermission> merged = new LinkedHashMap<>();
    for (String roleName : roleNames) {
      for (TablePermission p : getPermissionsForRole(schemaName, roleName)) {
        if (hasAnyPermission(p)) {
          merged.merge(p.table(), p, SqlRoleManager::mergePermissions);
        }
      }
    }
    expandWildcard(merged, schema.getTableNames());
    return new ArrayList<>(merged.values());
  }

  private List<TablePermission> getPermissionsForRole(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      return listTablePermissions(schemaName, roleName);
    }
    return getPermissionSet(schemaName, roleName).getTables().values().stream().toList();
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
              .delete(wildcard.delete())
              .reference(wildcard.reference()),
          SqlRoleManager::mergePermissions);
    }
  }

  private static boolean hasAnyPermission(TablePermission p) {
    return p.select() != SelectScope.NONE
        || p.insert() == UpdateScope.ALL
        || p.update() == UpdateScope.ALL
        || p.delete() == UpdateScope.ALL
        || p.reference() != ReferenceScope.NONE;
  }

  private static String selectScopeName(TablePermission tp) {
    return tp.select().name();
  }

  private static String updateScopeName(UpdateScope scope) {
    return scope.name();
  }

  private static String referenceScopeName(TablePermission tp) {
    return tp.reference().name();
  }

  private static TablePermission mergePermissions(TablePermission a, TablePermission b) {
    return new TablePermission(a.table())
        .select(higherSelectScope(a.select(), b.select()))
        .insert(
            a.insert() == UpdateScope.ALL || b.insert() == UpdateScope.ALL
                ? UpdateScope.ALL
                : UpdateScope.NONE)
        .update(
            a.update() == UpdateScope.ALL || b.update() == UpdateScope.ALL
                ? UpdateScope.ALL
                : UpdateScope.NONE)
        .delete(
            a.delete() == UpdateScope.ALL || b.delete() == UpdateScope.ALL
                ? UpdateScope.ALL
                : UpdateScope.NONE)
        .reference(higherReferenceScope(a.reference(), b.reference()));
  }

  private static SelectScope higherSelectScope(SelectScope a, SelectScope b) {
    return a.ordinal() >= b.ordinal() ? a : b;
  }

  private static ReferenceScope higherReferenceScope(ReferenceScope a, ReferenceScope b) {
    return a.ordinal() >= b.ordinal() ? a : b;
  }

  public void grantRoleToUser(Schema schema, String roleName, String username) {
    requireManagerOrOwner(schema);
    String schemaName = schema.getName();
    requireUserExists(username);
    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + username;
    database.getJooqAsAdmin(
        adminJooq -> {
          boolean isFirstRow = !membershipRowExists(adminJooq, schemaName, username, roleName);
          deleteGroupMembershipForRole(adminJooq, schemaName, username, roleName);
          upsertGroupMembership(adminJooq, username, schemaName, null, roleName);
          if (isFirstRow) {
            adminJooq.execute("GRANT {0} TO {1}", name(fullRole), name(fullUser));
          }
        });
    database.getListener().onSchemaChange();
  }

  public void revokeRoleFromUser(Schema schema, String roleName, String username) {
    requireManagerOrOwner(schema);
    String schemaName = schema.getName();
    if (!roleExists(schemaName, roleName)) return;
    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + username;
    database.getJooqAsAdmin(
        adminJooq -> {
          deleteGroupMembershipForRole(adminJooq, schemaName, username, roleName);
          adminJooq.execute("REVOKE {0} FROM {1}", name(fullRole), name(fullUser));
        });
    database.getListener().onSchemaChange();
  }

  public void addGroupMembership(
      String schemaName, String groupName, String userName, String roleName) {
    Schema schema = database.getSchema(schemaName);
    if (schema != null) requireManagerOrOwner(schema);
    if (isSystemRole(roleName)) {
      throw new MolgenisException(
          "system role '" + roleName + "' cannot be bound to group '" + groupName + "'");
    }
    requireGroupExists(schemaName, groupName);
    requireUserExists(userName);
    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + userName;
    database.getJooqAsAdmin(
        adminJooq -> {
          boolean isFirstRow = !membershipRowExists(adminJooq, schemaName, userName, roleName);
          upsertGroupMembership(adminJooq, userName, schemaName, groupName, roleName);
          if (isFirstRow) {
            adminJooq.execute("GRANT {0} TO {1}", name(fullRole), name(fullUser));
          }
        });
    database.getListener().onSchemaChange();
  }

  public void removeGroupMembership(
      String schemaName, String groupName, String userName, String roleName) {
    Schema schema = database.getSchema(schemaName);
    if (schema != null) requireManagerOrOwner(schema);
    String fullRole = fullRoleName(schemaName, roleName);
    String fullUser = MG_USER_PREFIX + userName;
    database.getJooqAsAdmin(
        adminJooq -> {
          deleteGroupMembership(adminJooq, userName, schemaName, groupName, roleName);
          if (!membershipRowExists(adminJooq, schemaName, userName, roleName)) {
            adminJooq.execute("REVOKE {0} FROM {1}", name(fullRole), name(fullUser));
          }
        });
    database.getListener().onSchemaChange();
  }

  public void setPermissions(Schema schema, String roleName, PermissionSet permissions) {
    requireManagerOrOwner(schema);
    String schemaName = schema.getName();
    if (permissions.getSchema() != null && !permissions.getSchema().equals(schemaName)) {
      throw new MolgenisException(
          "PermissionSet schema '"
              + permissions.getSchema()
              + "' does not match target schema '"
              + schemaName
              + "'");
    }
    if (isSystemRole(roleName)) {
      throw new MolgenisException("System roles are immutable: cannot modify '" + roleName + "'");
    }
    for (Map.Entry<String, TablePermission> entry : permissions.getTables().entrySet()) {
      TableMetadata tableMeta = schema.getMetadata().getTableMetadata(entry.getKey());
      if (tableMeta != null) {
        tableMeta.rejectIfRlsScopeWithoutRls(entry.getValue(), permissions);
      }
    }
    database.getJooqAsAdmin(
        adminJooq -> {
          deleteAllRolePermissions(adminJooq, schemaName, roleName);
          for (Map.Entry<String, TablePermission> entry : permissions.getTables().entrySet()) {
            String tableName = entry.getKey();
            TablePermission tp = entry.getValue();
            upsertRolePermission(
                adminJooq,
                schemaName,
                roleName,
                tableName,
                selectScopeName(tp),
                updateScopeName(tp.insert()),
                updateScopeName(tp.update()),
                updateScopeName(tp.delete()),
                referenceScopeName(tp),
                permissions.isChangeOwner(),
                permissions.isChangeGroup(),
                permissions.getDescription());
          }
        });
    String fullRole = fullRoleName(schemaName, roleName);
    Collection<String> existingTables = database.getSchema(schemaName).getTableNames();
    for (Map.Entry<String, TablePermission> entry : permissions.getTables().entrySet()) {
      String tableName = entry.getKey();
      if (!existingTables.contains(tableName)) continue;
      TablePermission tp = entry.getValue();
      org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
      jooq().execute("REVOKE ALL ON {0} FROM {1}", jooqTable, name(fullRole));
      TablePermission pg =
          new TablePermission(tableName)
              .select(tp.select() != SelectScope.NONE ? SelectScope.ALL : SelectScope.NONE)
              .insert(tp.insert() != UpdateScope.NONE ? UpdateScope.ALL : UpdateScope.NONE)
              .update(tp.update() != UpdateScope.NONE ? UpdateScope.ALL : UpdateScope.NONE)
              .delete(tp.delete() != UpdateScope.NONE ? UpdateScope.ALL : UpdateScope.NONE);
      applyPgGrants(schemaName, fullRole, tableName, pg);
    }
    database.getListener().onSchemaChange();
  }

  public PermissionSet getPermissionSet(Schema schema, String roleName) {
    return getPermissionSet(schema.getName(), roleName);
  }

  public PermissionSet getPermissions(Schema schema, String roleName) {
    return getPermissionSet(schema.getName(), roleName);
  }

  public PermissionSet getPermissionSet(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      return synthesizeSystemPermissionSet(roleName);
    }
    PermissionSet result = new PermissionSet();
    database.getJooqAsAdmin(
        adminJooq ->
            loadPermissionSet(adminJooq, schemaName, roleName)
                .forEach(
                    row -> {
                      TablePermission tp =
                          new TablePermission(row.get(RPM_TABLE_NAME))
                              .select(SelectScope.fromString(row.get(RPM_SELECT_SCOPE)))
                              .insert(UpdateScope.fromString(row.get(RPM_INSERT_SCOPE)))
                              .update(UpdateScope.fromString(row.get(RPM_UPDATE_SCOPE)))
                              .delete(UpdateScope.fromString(row.get(RPM_DELETE_SCOPE)))
                              .reference(
                                  PermissionSet.ReferenceScope.fromString(
                                      row.get(RPM_REFERENCE_SCOPE)));
                      result.putTable(row.get(RPM_TABLE_NAME), tp);
                      result.setChangeOwner(Boolean.TRUE.equals(row.get(RPM_CHANGE_OWNER)));
                      result.setChangeGroup(Boolean.TRUE.equals(row.get(RPM_CHANGE_GROUP)));
                      String desc = row.get(RPM_DESCRIPTION);
                      if (desc != null && !desc.isEmpty()) {
                        result.setDescription(desc);
                      }
                    }));
    return result;
  }

  private PermissionSet synthesizeSystemPermissionSet(String roleName) {
    List<TablePermission> perms = systemPermissions(roleName);
    boolean canChangeOwnerAndGroup =
        roleName.equals(Privileges.MANAGER.toString())
            || roleName.equals(Privileges.OWNER.toString());
    PermissionSet ps =
        new PermissionSet()
            .setDescription(roleName)
            .setChangeOwner(canChangeOwnerAndGroup)
            .setChangeGroup(canChangeOwnerAndGroup);
    if (!perms.isEmpty()) {
      ps.putTable(perms.get(0).table(), perms.get(0));
    }
    return ps;
  }

  public Set<SelectScope> getEffectiveSelectScopes(Schema schema, SqlTableMetadata table) {
    return getEffectiveSelectScopes(schema.getName(), table);
  }

  public Set<SelectScope> getEffectiveSelectScopes(String schemaName, SqlTableMetadata table) {
    Set<SelectScope> result = EnumSet.noneOf(SelectScope.class);
    addCustomRoleScope(schemaName, table, result);
    addSystemRoleScopes(schemaName, result);
    return result;
  }

  public SelectScope getCustomRoleSelectScope(String schemaName, SqlTableMetadata table) {
    Set<SelectScope> customScopes = EnumSet.noneOf(SelectScope.class);
    addCustomRoleScope(schemaName, table, customScopes);
    return customScopes.isEmpty() ? null : customScopes.iterator().next();
  }

  public boolean hasCustomRoleForUser(String schemaName) {
    String activeUser = database.getActiveUser();
    if (activeUser == null) return false;
    boolean[] found = {false};
    database.getJooqAsAdmin(
        adminJooq -> {
          found[0] =
              adminJooq.fetchExists(
                  adminJooq
                      .select()
                      .from(GROUP_MEMBERSHIP_METADATA)
                      .where(GMM_SCHEMA_NAME.eq(schemaName), GMM_USER_NAME.eq(activeUser)));
          if (!found[0]) {
            String fullUser = MG_USER_PREFIX + activeUser;
            String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
            found[0] = !findDirectCustomRolesForUser(adminJooq, schemaName, fullUser).isEmpty();
          }
        });
    return found[0];
  }

  private List<String> findDirectCustomRolesForUser(
      DSLContext adminJooq, String schemaName, String fullUser) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    return adminJooq
        .fetch(
            "SELECT r.rolname FROM pg_auth_members am "
                + "JOIN pg_roles r ON r.oid = am.roleid "
                + "JOIN pg_roles m ON m.oid = am.member "
                + "WHERE m.rolname = {0} AND r.rolname LIKE {1}",
            inline(fullUser), inline(rolePrefix + "%"))
        .stream()
        .map(r -> r.get(0, String.class).substring(rolePrefix.length()))
        .filter(role -> !Privileges.isSystemRole(role))
        .toList();
  }

  private void addCustomRoleScope(
      String schemaName, SqlTableMetadata table, Set<SelectScope> result) {
    String activeUser = database.getActiveUser();
    if (activeUser == null) return;
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .selectDistinct(RPM_SELECT_SCOPE)
                .from(GROUP_MEMBERSHIP_METADATA)
                .join(ROLE_PERMISSION_METADATA)
                .on(
                    GMM_SCHEMA_NAME.eq(RPM_SCHEMA_NAME),
                    GMM_ROLE_NAME.eq(RPM_ROLE_NAME),
                    RPM_TABLE_NAME.eq(table.getTableName()))
                .where(GMM_USER_NAME.eq(activeUser), GMM_SCHEMA_NAME.eq(schemaName))
                .fetchStream()
                .forEach(
                    row -> {
                      SelectScope scope = SelectScope.fromString(row.get(RPM_SELECT_SCOPE));
                      if (scope != SelectScope.NONE) {
                        result.add(scope);
                      }
                    }));
    String fullUser = MG_USER_PREFIX + activeUser;
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .fetch(
                    "SELECT DISTINCT select_scope FROM \"MOLGENIS\".role_permission_metadata"
                        + " WHERE schema_name = ? AND table_name = ?"
                        + " AND pg_has_role(?, 'MG_ROLE_' || schema_name || '/' || role_name, 'MEMBER')",
                    schemaName,
                    table.getTableName(),
                    fullUser)
                .forEach(
                    row -> {
                      SelectScope scope =
                          SelectScope.fromString(row.get("select_scope", String.class));
                      if (scope != SelectScope.NONE) {
                        result.add(scope);
                      }
                    }));
  }

  private void addSystemRoleScopes(String schemaName, Set<SelectScope> result) {
    String activeUser = database.getActiveUser();
    if (activeUser == null) return;
    List<String> directRoles = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq ->
            directRoles.addAll(findDirectSystemRolesForUser(adminJooq, schemaName, activeUser)));
    for (String role : directRoles) {
      if (role.equals(Privileges.COUNT.toString())) {
        result.add(SelectScope.COUNT);
      } else if (role.equals(Privileges.AGGREGATOR.toString())) {
        result.add(SelectScope.AGGREGATE);
      } else if (role.equals(Privileges.RANGE.toString())) {
        result.add(SelectScope.RANGE);
      } else if (role.equals(Privileges.EXISTS.toString())) {
        result.add(SelectScope.EXISTS);
      } else if (role.equals(Privileges.VIEWER.toString())
          || role.equals(Privileges.EDITOR.toString())
          || role.equals(Privileges.MANAGER.toString())
          || role.equals(Privileges.OWNER.toString())) {
        result.add(SelectScope.ALL);
      }
    }
  }

  private static List<String> findDirectSystemRolesForUser(
      DSLContext jooq, String schemaName, String username) {
    String fullUser = MG_USER_PREFIX + username;
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    return jooq
        .fetch(
            "SELECT r.rolname FROM pg_auth_members am "
                + "JOIN pg_roles r ON r.oid = am.roleid "
                + "JOIN pg_roles m ON m.oid = am.member "
                + "WHERE m.rolname = {0} AND r.rolname LIKE {1}",
            inline(fullUser), inline(rolePrefix + "%"))
        .stream()
        .map(r -> r.get(0, String.class).substring(rolePrefix.length()))
        .filter(Privileges::isSystemRole)
        .toList();
  }

  public void createGroup(Schema schema, String groupName) {
    requireManagerOrOwner(schema);
    String schemaName = schema.getName();
    database.getJooqAsAdmin(
        adminJooq -> {
          if (groupExists(adminJooq, schemaName, groupName)) {
            throw new MolgenisException(
                "Group '" + groupName + "' already exists in schema '" + schemaName + "'");
          }
          insertGroup(adminJooq, schemaName, groupName);
        });
    database.getListener().onSchemaChange();
  }

  public void deleteGroup(Schema schema, String groupName) {
    requireManagerOrOwner(schema);
    String schemaName = schema.getName();
    database.getJooqAsAdmin(
        adminJooq -> {
          int deleted = MetadataUtils.deleteGroup(adminJooq, schemaName, groupName);
          if (deleted == 0) {
            throw new MolgenisException(
                "Group '" + groupName + "' not found in schema '" + schemaName + "'");
          }
        });
    database.getListener().onSchemaChange();
  }

  public void addGroupMember(Schema schema, String groupName, String username) {
    requireManagerOrOwner(schema);
    String schemaName = schema.getName();
    requireGroupExists(schemaName, groupName);
    requireUserExists(username);
    database.getJooqAsAdmin(
        adminJooq ->
            upsertGroupMembership(
                adminJooq, username, schemaName, groupName, GROUP_MEMBERSHIP_SENTINEL_ROLE));
    database.getListener().onSchemaChange();
  }

  public void removeGroupMember(Schema schema, String groupName, String username) {
    requireManagerOrOwner(schema);
    String schemaName = schema.getName();
    database.getJooqAsAdmin(
        adminJooq ->
            deleteGroupMembership(
                adminJooq, username, schemaName, groupName, GROUP_MEMBERSHIP_SENTINEL_ROLE));
    database.getListener().onSchemaChange();
  }

  public List<Group> listGroups(Schema schema) {
    String schemaName = schema.getName();
    List<Group> result = new ArrayList<>();
    database.getJooqAsAdmin(adminJooq -> result.addAll(fetchGroups(adminJooq, schemaName)));
    return result;
  }

  private void requireGroupExists(String schemaName, String groupName) {
    database.getJooqAsAdmin(
        adminJooq -> requireGroupExistsViaJooq(adminJooq, schemaName, groupName));
  }

  private void requireUserExists(String username) {
    database.getJooqAsAdmin(adminJooq -> MetadataUtils.requireUserExists(adminJooq, username));
  }

  private static void requireGroupExistsViaJooq(
      DSLContext adminJooq, String schemaName, String groupName) {
    if (!groupExists(adminJooq, schemaName, groupName)) {
      throw new MolgenisException(
          "Group '" + groupName + "' not found in schema '" + schemaName + "'");
    }
  }

  private List<TablePermission> systemPermissions(String roleName) {
    if (roleName.equals(Privileges.EXISTS.toString())
        || roleName.equals(Privileges.RANGE.toString())
        || roleName.equals(Privileges.AGGREGATOR.toString())
        || roleName.equals(Privileges.COUNT.toString())) {
      return List.of(new TablePermission("*"));
    } else if (roleName.equals(Privileges.VIEWER.toString())) {
      return List.of(new TablePermission("*").select(SelectScope.ALL));
    } else if (roleName.equals(Privileges.EDITOR.toString())
        || roleName.equals(Privileges.MANAGER.toString())
        || roleName.equals(Privileges.OWNER.toString())) {
      return List.of(
          new TablePermission("*")
              .select(SelectScope.ALL)
              .insert(UpdateScope.ALL)
              .update(UpdateScope.ALL)
              .delete(UpdateScope.ALL));
    }
    return List.of();
  }

  public void enableRlsForTable(Schema schema, String tableName) {
    String schemaName = schema.getName();
    boolean[] alreadyEnabled = {false};
    boolean[] tableExists = {false};
    database.getJooqAsAdmin(
        adminJooq -> {
          org.jooq.Record pgRecord =
              adminJooq.fetchOne(
                  "SELECT c.relrowsecurity FROM pg_class c"
                      + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                      + " WHERE n.nspname = ? AND c.relname = ?",
                  schemaName,
                  tableName);
          if (pgRecord != null) {
            tableExists[0] = true;
            alreadyEnabled[0] = Boolean.TRUE.equals(pgRecord.get(0, Boolean.class));
          }
        });
    if (!tableExists[0]) {
      logger.debug("Table {}.{} not found in pg_class; skipping RLS enable", schemaName, tableName);
      return;
    }
    if (alreadyEnabled[0]) {
      return;
    }
    logger.debug("Enabling RLS for {}.{}", schemaName, tableName);
    TableMetadata tableMetadata = schema.getTable(tableName).getMetadata();
    database.getJooqAsAdmin(
        adminJooq -> {
          adminJooq.execute(
              "ALTER TABLE {0} ADD COLUMN IF NOT EXISTS mg_owner TEXT",
              name(schemaName, tableName));
          adminJooq.execute(
              "DO $$ BEGIN "
                  + "ALTER TABLE {0} ADD CONSTRAINT {1} "
                  + "FOREIGN KEY (mg_owner) "
                  + "REFERENCES \"MOLGENIS\".users_metadata(username) "
                  + "ON DELETE SET NULL ON UPDATE CASCADE; "
                  + "EXCEPTION WHEN duplicate_object THEN NULL; END $$",
              name(schemaName, tableName), name(tableName + "_mg_owner_fk"));
          adminJooq.execute(
              "ALTER TABLE {0} ADD COLUMN IF NOT EXISTS mg_groups TEXT[]",
              name(schemaName, tableName));
          if (tableMetadata.getInheritName() == null) {
            registerRlsColumnMetadata(adminJooq, tableMetadata);
          }
          adminJooq.execute(
              "CREATE INDEX IF NOT EXISTS {0} ON {1} (mg_owner) WHERE mg_owner IS NOT NULL",
              name(tableName + "_mg_owner_idx"), name(schemaName, tableName));
          adminJooq.execute(
              "CREATE INDEX IF NOT EXISTS {0} ON {1} USING GIN (mg_groups)",
              name(tableName + "_mg_groups_idx"), name(schemaName, tableName));
          adminJooq.execute(
              "ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", name(schemaName, tableName));
          adminJooq.execute(
              "ALTER TABLE {0} FORCE ROW LEVEL SECURITY", name(schemaName, tableName));
          String selectPolicy = tablePolicyName(tableName, "select");
          String insertPolicy = tablePolicyName(tableName, "insert");
          String updatePolicy = tablePolicyName(tableName, "update");
          String deletePolicy = tablePolicyName(tableName, "delete");
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(selectPolicy), name(schemaName, tableName));
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(insertPolicy), name(schemaName, tableName));
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(updatePolicy), name(schemaName, tableName));
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(deletePolicy), name(schemaName, tableName));
          adminJooq.execute(
              "CREATE POLICY {0} ON {1} FOR SELECT"
                  + " USING ( \"MOLGENIS\".mg_can_read('"
                  + schemaName
                  + "', '"
                  + tableName
                  + "', mg_groups, mg_owner) )",
              name(selectPolicy),
              name(schemaName, tableName));
          adminJooq.execute(
              "CREATE POLICY {0} ON {1} FOR INSERT"
                  + " WITH CHECK ("
                  + " \"MOLGENIS\".mg_can_write('"
                  + schemaName
                  + "', '"
                  + tableName
                  + "', mg_groups, mg_owner, 'insert')"
                  + " AND \"MOLGENIS\".mg_can_write_all('"
                  + schemaName
                  + "', '"
                  + tableName
                  + "', mg_groups, mg_owner, 'insert', FALSE, FALSE)"
                  + " )",
              name(insertPolicy),
              name(schemaName, tableName));
          adminJooq.execute(
              "CREATE POLICY {0} ON {1} FOR UPDATE"
                  + " USING ( \"MOLGENIS\".mg_can_write('"
                  + schemaName
                  + "', '"
                  + tableName
                  + "', mg_groups, mg_owner, 'update') )"
                  + " WITH CHECK ( \"MOLGENIS\".mg_can_write_all('"
                  + schemaName
                  + "', '"
                  + tableName
                  + "', mg_groups, mg_owner, 'update', FALSE, FALSE) )",
              name(updatePolicy),
              name(schemaName, tableName));
          adminJooq.execute(
              "CREATE POLICY {0} ON {1} FOR DELETE"
                  + " USING ( \"MOLGENIS\".mg_can_write('"
                  + schemaName
                  + "', '"
                  + tableName
                  + "', mg_groups, mg_owner, 'delete') )",
              name(deletePolicy),
              name(schemaName, tableName));
          String changeTrigger = changeCapabilityTriggerName(tableName);
          adminJooq.execute(
              "DROP TRIGGER IF EXISTS {0} ON {1}",
              name(changeTrigger), name(schemaName, tableName));
          adminJooq.execute(
              "CREATE TRIGGER {0}"
                  + " BEFORE INSERT OR UPDATE ON {1}"
                  + " FOR EACH ROW EXECUTE FUNCTION"
                  + " \"MOLGENIS\".mg_check_change_capability('"
                  + schemaName
                  + "', '"
                  + tableName
                  + "')",
              name(changeTrigger),
              name(schemaName, tableName));
        });
  }

  public void disableRlsForTable(Schema schema, String tableName) {
    String schemaName = schema.getName();
    boolean[] tableExists = {false};
    database.getJooqAsAdmin(
        adminJooq -> {
          org.jooq.Record pgRecord =
              adminJooq.fetchOne(
                  "SELECT 1 FROM pg_class c"
                      + " JOIN pg_namespace n ON n.oid = c.relnamespace"
                      + " WHERE n.nspname = ? AND c.relname = ?",
                  schemaName,
                  tableName);
          tableExists[0] = pgRecord != null;
        });
    if (!tableExists[0]) {
      logger.debug(
          "Table {}.{} not found in pg_class; skipping RLS disable", schemaName, tableName);
      return;
    }
    logger.debug("Disabling RLS for {}.{}", schemaName, tableName);
    database.getJooqAsAdmin(
        adminJooq -> {
          String selectPolicy = tablePolicyName(tableName, "select");
          String insertPolicy = tablePolicyName(tableName, "insert");
          String updatePolicy = tablePolicyName(tableName, "update");
          String deletePolicy = tablePolicyName(tableName, "delete");
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(selectPolicy), name(schemaName, tableName));
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(insertPolicy), name(schemaName, tableName));
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(updatePolicy), name(schemaName, tableName));
          adminJooq.execute(
              "DROP POLICY IF EXISTS {0} ON {1}", name(deletePolicy), name(schemaName, tableName));
          String changeTrigger = changeCapabilityTriggerName(tableName);
          adminJooq.execute(
              "DROP TRIGGER IF EXISTS {0} ON {1}",
              name(changeTrigger), name(schemaName, tableName));
          adminJooq.execute(
              "ALTER TABLE {0} DISABLE ROW LEVEL SECURITY", name(schemaName, tableName));
        });
  }

  static void registerRlsColumnMetadata(DSLContext adminJooq, TableMetadata tableMetadata) {
    Column ownerCol = new Column(tableMetadata, MG_OWNER_COLUMN).setType(STRING).setPosition(-7);
    Column groupsCol =
        new Column(tableMetadata, MG_GROUPS_COLUMN).setType(STRING_ARRAY).setPosition(-6);
    saveColumnMetadata(adminJooq, ownerCol);
    saveColumnMetadata(adminJooq, groupsCol);
  }

  static void deregisterRlsColumnMetadata(DSLContext adminJooq, TableMetadata tableMetadata) {
    Column ownerCol = new Column(tableMetadata, MG_OWNER_COLUMN);
    Column groupsCol = new Column(tableMetadata, MG_GROUPS_COLUMN);
    deleteColumn(adminJooq, ownerCol);
    deleteColumn(adminJooq, groupsCol);
  }

  private static String tablePolicyName(String tableName, String verb) {
    String candidate = "mg_p_" + tableName + "_" + verb;
    if (candidate.getBytes(UTF_8).length <= PG_MAX_ID_LENGTH) {
      return candidate;
    }
    String hash = pgIdentifierHash(tableName + "_" + verb).substring(0, 12);
    return "mg_p_" + hash;
  }

  private static String changeCapabilityTriggerName(String tableName) {
    String candidate = "mg_check_change_cap_" + tableName;
    if (candidate.getBytes(UTF_8).length <= PG_MAX_ID_LENGTH) {
      return candidate;
    }
    String hash = pgIdentifierHash(tableName).substring(0, 12);
    return "mg_check_change_cap_" + hash;
  }

  void rejectDisableIfPermissionsExist(String schemaName, String tableName) {
    boolean[] hasPermissions = {false};
    database.getJooqAsAdmin(
        adminJooq -> hasPermissions[0] = rolePermissionExists(adminJooq, schemaName, tableName));
    if (hasPermissions[0]) {
      throw new MolgenisException(
          "Cannot disable RLS: first remove permissions on '" + schemaName + "." + tableName + "'");
    }
  }

  private static String pgIdentifierHash(String input) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-1");
      byte[] digest = md.digest(input.getBytes(UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : digest) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new MolgenisException("SHA-1 not available", e);
    }
  }
}
