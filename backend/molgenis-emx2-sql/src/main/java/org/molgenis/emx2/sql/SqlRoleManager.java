package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.MetadataUtils.*;

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
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlRoleManager {

  private static final Logger logger = LoggerFactory.getLogger(SqlRoleManager.class);

  public static final int PG_MAX_ID_LENGTH = 63;

  private static final String SYSTEM_ROLE_OWNER = "Owner";
  private static final String SYSTEM_ROLE_MANAGER = "Manager";
  private static final String SYSTEM_ROLE_EDITOR = "Editor";
  private static final String SYSTEM_ROLE_VIEWER = "Viewer";

  private static final List<String> SYSTEM_ROLE_NAMES =
      List.of(SYSTEM_ROLE_OWNER, SYSTEM_ROLE_MANAGER, SYSTEM_ROLE_EDITOR, SYSTEM_ROLE_VIEWER);

  static final String DIRECT_GRANT_GROUP = "__direct__";

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

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  public static String memberRoleName(String schemaName) {
    return MG_ROLE_PREFIX + schemaName + "_MEMBER";
  }

  public void createRole(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot create system role: " + roleName);
    }
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
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .insertInto(ROLE_PERMISSION_METADATA)
                .columns(RPM_SCHEMA_NAME, RPM_ROLE_NAME, RPM_TABLE_NAME)
                .values(schemaName, roleName, RPM_STUB_TABLE_SENTINEL)
                .onConflictDoNothing()
                .execute());
  }

  public void createRole(Schema schema, String roleName, String description) {
    createRole(schema.getName(), roleName);
  }

  public void deleteRole(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot delete system role: " + roleName);
    }
    List<String> affectedUsers = findUsersInSchemaWithRole(schemaName, roleName);
    database.getJooqAsAdmin(
        adminJooq -> {
          adminJooq
              .deleteFrom(ROLE_PERMISSION_METADATA)
              .where(RPM_SCHEMA_NAME.eq(schemaName), RPM_ROLE_NAME.eq(roleName))
              .execute();
          adminJooq
              .deleteFrom(GROUP_MEMBERSHIP_METADATA)
              .where(GMM_SCHEMA_NAME.eq(schemaName), GMM_ROLE_NAME.eq(roleName))
              .execute();
        });
    for (String userName : affectedUsers) {
      if (!userHasAnyGroupMembershipInSchema(schemaName, userName)) {
        revokeMemberRoleFromUser(schemaName, userName);
      }
    }
    database.getListener().onSchemaChange();
  }

  public void deleteRole(Schema schema, String roleName) {
    deleteRole(schema.getName(), roleName);
  }

  private List<String> findUsersInSchemaWithRole(String schemaName, String roleName) {
    List<String> result = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .select(GMM_USER_NAME)
                .from(GROUP_MEMBERSHIP_METADATA)
                .where(GMM_SCHEMA_NAME.eq(schemaName), GMM_ROLE_NAME.eq(roleName))
                .fetchStream()
                .map(row -> row.get(GMM_USER_NAME))
                .forEach(result::add));
    return result;
  }

  private boolean userHasAnyGroupMembershipInSchema(String schemaName, String userName) {
    boolean[] found = {false};
    database.getJooqAsAdmin(
        adminJooq ->
            found[0] =
                adminJooq.fetchExists(
                    adminJooq
                        .select()
                        .from(GROUP_MEMBERSHIP_METADATA)
                        .where(GMM_SCHEMA_NAME.eq(schemaName), GMM_USER_NAME.eq(userName))));
    return found[0];
  }

  private void grantMemberRoleToUser(String schemaName, String userName) {
    String memberRole = memberRoleName(schemaName);
    String fullUser = MG_USER_PREFIX + userName;
    database.getJooqAsAdmin(
        adminJooq -> adminJooq.execute("GRANT {0} TO {1}", name(memberRole), name(fullUser)));
  }

  private void revokeMemberRoleFromUser(String schemaName, String userName) {
    String memberRole = memberRoleName(schemaName);
    String fullUser = MG_USER_PREFIX + userName;
    database.getJooqAsAdmin(
        adminJooq -> adminJooq.execute("REVOKE {0} FROM {1}", name(memberRole), name(fullUser)));
  }

  public void addGroupMembership(
      String schemaName, String groupName, String userName, String roleName) {
    if (DIRECT_GRANT_GROUP.equals(groupName)) {
      throw new MolgenisException("Reserved group name '__direct__' cannot be used");
    }
    requireGroupExists(schemaName, groupName);
    requireUserExists(userName);
    insertGroupMembership(schemaName, groupName, userName, roleName);
    grantMemberRoleToUser(schemaName, userName);
    database.getListener().onSchemaChange();
  }

  private void insertGroupMembership(
      String schemaName, String groupName, String userName, String roleName) {
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .insertInto(GROUP_MEMBERSHIP_METADATA)
                .columns(GMM_USER_NAME, GMM_SCHEMA_NAME, GMM_GROUP_NAME, GMM_ROLE_NAME)
                .values(userName, schemaName, groupName, roleName)
                .onConflictDoNothing()
                .execute());
  }

  public void removeGroupMembership(
      String schemaName, String groupName, String userName, String roleName) {
    if (DIRECT_GRANT_GROUP.equals(groupName)) {
      throw new MolgenisException("Reserved group name '__direct__' cannot be used");
    }
    deleteGroupMembership(schemaName, groupName, userName, roleName);
    if (!userHasAnyGroupMembershipInSchema(schemaName, userName)) {
      revokeMemberRoleFromUser(schemaName, userName);
    }
    database.getListener().onSchemaChange();
  }

  private void deleteGroupMembership(
      String schemaName, String groupName, String userName, String roleName) {
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .deleteFrom(GROUP_MEMBERSHIP_METADATA)
                .where(
                    GMM_USER_NAME.eq(userName),
                    GMM_SCHEMA_NAME.eq(schemaName),
                    GMM_GROUP_NAME.eq(groupName),
                    GMM_ROLE_NAME.eq(roleName))
                .execute());
  }

  public void setPermissions(Schema schema, String roleName, PermissionSet permissions) {
    String schemaName = schema.getName();
    if (permissions.getSchema() != null && !permissions.getSchema().equals(schemaName)) {
      throw new MolgenisException(
          "PermissionSet schema '"
              + permissions.getSchema()
              + "' does not match target schema '"
              + schemaName
              + "'");
    }
    if (SYSTEM_ROLE_NAMES.contains(roleName)) {
      throw new MolgenisException("System role permissions are immutable: " + roleName);
    }
    for (Map.Entry<String, PermissionSet.TablePermissions> entry :
        permissions.getTables().entrySet()) {
      rejectRlsScopeOnNonRlsTable(schema, entry.getKey(), entry.getValue(), permissions);
    }
    database.getJooqAsAdmin(
        adminJooq -> {
          adminJooq
              .deleteFrom(ROLE_PERMISSION_METADATA)
              .where(
                  RPM_SCHEMA_NAME.eq(schemaName),
                  RPM_ROLE_NAME.eq(roleName),
                  RPM_TABLE_NAME.ne("*"))
              .execute();
          for (Map.Entry<String, PermissionSet.TablePermissions> entry :
              permissions.getTables().entrySet()) {
            String tableName = entry.getKey();
            PermissionSet.TablePermissions tp = entry.getValue();
            adminJooq
                .insertInto(ROLE_PERMISSION_METADATA)
                .columns(
                    RPM_SCHEMA_NAME,
                    RPM_ROLE_NAME,
                    RPM_TABLE_NAME,
                    RPM_SELECT_SCOPE,
                    RPM_INSERT_SCOPE,
                    RPM_UPDATE_SCOPE,
                    RPM_DELETE_SCOPE,
                    RPM_CHANGE_OWNER,
                    RPM_CHANGE_GROUP,
                    RPM_DESCRIPTION)
                .values(
                    schemaName,
                    roleName,
                    tableName,
                    tp.getSelect().name(),
                    tp.getInsert().name(),
                    tp.getUpdate().name(),
                    tp.getDelete().name(),
                    permissions.isChangeOwner(),
                    permissions.isChangeGroup(),
                    permissions.getDescription())
                .onConflict(RPM_SCHEMA_NAME, RPM_ROLE_NAME, RPM_TABLE_NAME)
                .doUpdate()
                .set(RPM_SELECT_SCOPE, tp.getSelect().name())
                .set(RPM_INSERT_SCOPE, tp.getInsert().name())
                .set(RPM_UPDATE_SCOPE, tp.getUpdate().name())
                .set(RPM_DELETE_SCOPE, tp.getDelete().name())
                .set(RPM_CHANGE_OWNER, permissions.isChangeOwner())
                .set(RPM_CHANGE_GROUP, permissions.isChangeGroup())
                .set(RPM_DESCRIPTION, permissions.getDescription())
                .execute();
          }
          if (permissions.getTables().isEmpty()) {
            adminJooq
                .insertInto(ROLE_PERMISSION_METADATA)
                .columns(RPM_SCHEMA_NAME, RPM_ROLE_NAME, RPM_TABLE_NAME)
                .values(schemaName, roleName, RPM_STUB_TABLE_SENTINEL)
                .onConflictDoNothing()
                .execute();
          }
        });
    database.getListener().onSchemaChange();
  }

  private static final String RLS_SCOPE_ERROR =
      "OWN/GROUP scope requires RLS-enabled table; enable RLS on '%s.%s' first";

  private void rejectRlsScopeOnNonRlsTable(
      Schema schema,
      String tableName,
      PermissionSet.TablePermissions tp,
      PermissionSet permissions) {
    TableMetadata meta = schema.getMetadata().getTableMetadata(tableName);
    if (meta == null || meta.getRlsEnabled()) {
      return;
    }
    boolean hasRlsScope =
        tp.getSelect() == SelectScope.OWN
            || tp.getSelect() == SelectScope.GROUP
            || tp.getUpdate() == UpdateScope.OWN
            || tp.getUpdate() == UpdateScope.GROUP
            || tp.getInsert() == UpdateScope.OWN
            || tp.getInsert() == UpdateScope.GROUP
            || tp.getDelete() == UpdateScope.OWN
            || tp.getDelete() == UpdateScope.GROUP;
    boolean hasChangeFlag = permissions.isChangeOwner() || permissions.isChangeGroup();
    if (hasRlsScope || hasChangeFlag) {
      throw new MolgenisException(String.format(RLS_SCOPE_ERROR, schema.getName(), tableName));
    }
  }

  public PermissionSet getPermissionSet(Schema schema, String roleName) {
    return getPermissionSet(schema.getName(), roleName);
  }

  public PermissionSet getPermissions(Schema schema, String roleName) {
    return getPermissionSet(schema.getName(), roleName);
  }

  public PermissionSet getPermissionSet(String schemaName, String roleName) {
    PermissionSet result = new PermissionSet();
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .select(
                    RPM_TABLE_NAME,
                    RPM_SELECT_SCOPE,
                    RPM_INSERT_SCOPE,
                    RPM_UPDATE_SCOPE,
                    RPM_DELETE_SCOPE,
                    RPM_CHANGE_OWNER,
                    RPM_CHANGE_GROUP,
                    RPM_DESCRIPTION)
                .from(ROLE_PERMISSION_METADATA)
                .where(
                    RPM_SCHEMA_NAME.eq(schemaName),
                    RPM_ROLE_NAME.eq(roleName),
                    RPM_TABLE_NAME.ne("*"),
                    RPM_TABLE_NAME.ne(RPM_STUB_TABLE_SENTINEL))
                .fetchStream()
                .forEach(
                    row -> {
                      PermissionSet.TablePermissions tp = new PermissionSet.TablePermissions();
                      tp.setSelect(SelectScope.fromString(row.get(RPM_SELECT_SCOPE)));
                      tp.setInsert(UpdateScope.fromString(row.get(RPM_INSERT_SCOPE)));
                      tp.setUpdate(UpdateScope.fromString(row.get(RPM_UPDATE_SCOPE)));
                      tp.setDelete(UpdateScope.fromString(row.get(RPM_DELETE_SCOPE)));
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

  public List<String> listRoles(Schema schema) {
    return listRoles(schema.getName());
  }

  public List<String> listRoles(String schemaName) {
    List<String> result = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .selectDistinct(RPM_ROLE_NAME)
                .from(ROLE_PERMISSION_METADATA)
                .where(
                    RPM_SCHEMA_NAME.eq(schemaName),
                    RPM_ROLE_NAME.notIn(SYSTEM_ROLE_NAMES),
                    RPM_TABLE_NAME.ne(RPM_STUB_TABLE_SENTINEL))
                .fetchStream()
                .map(row -> row.get(RPM_ROLE_NAME))
                .forEach(result::add));
    return result;
  }

  public boolean roleExists(String schemaName, String roleName) {
    if (SYSTEM_ROLE_NAMES.contains(roleName)) {
      return true;
    }
    boolean[] found = {false};
    database.getJooqAsAdmin(
        adminJooq ->
            found[0] =
                adminJooq.fetchExists(
                    adminJooq
                        .select()
                        .from(ROLE_PERMISSION_METADATA)
                        .where(RPM_SCHEMA_NAME.eq(schemaName), RPM_ROLE_NAME.eq(roleName))));
    return found[0];
  }

  public void grant(String schemaName, String roleName, TablePermission permission) {
    if (SYSTEM_ROLE_NAMES.contains(roleName)) {
      throw new MolgenisException("Cannot grant custom permissions to system role: " + roleName);
    }
    String tableName = permission.table();
    if (tableName == null) {
      throw new MolgenisException("Table name is required for table-level grant");
    }
    Schema schema = database.getSchema(schemaName);
    if (schema == null || !schema.getTableNames().contains(tableName)) {
      throw new MolgenisException("Table does not exist: " + tableName);
    }
    database.getJooqAsAdmin(
        adminJooq -> {
          Record existing =
              adminJooq
                  .select(
                      RPM_SELECT_SCOPE,
                      RPM_INSERT_SCOPE,
                      RPM_UPDATE_SCOPE,
                      RPM_DELETE_SCOPE,
                      RPM_CHANGE_OWNER,
                      RPM_CHANGE_GROUP)
                  .from(ROLE_PERMISSION_METADATA)
                  .where(
                      RPM_SCHEMA_NAME.eq(schemaName),
                      RPM_ROLE_NAME.eq(roleName),
                      RPM_TABLE_NAME.eq(tableName))
                  .fetchOne();
          String selectScope =
              mergeScope(
                  existing == null ? null : existing.get(RPM_SELECT_SCOPE),
                  permission.select(),
                  SelectScope.NONE.name());
          String insertScope =
              mergeScope(
                  existing == null ? null : existing.get(RPM_INSERT_SCOPE),
                  permission.insert(),
                  UpdateScope.NONE.name());
          String updateScope =
              mergeScope(
                  existing == null ? null : existing.get(RPM_UPDATE_SCOPE),
                  permission.update(),
                  UpdateScope.NONE.name());
          String deleteScope =
              mergeScope(
                  existing == null ? null : existing.get(RPM_DELETE_SCOPE),
                  permission.delete(),
                  UpdateScope.NONE.name());
          boolean changeOwner =
              existing != null && Boolean.TRUE.equals(existing.get(RPM_CHANGE_OWNER));
          boolean changeGroup =
              existing != null && Boolean.TRUE.equals(existing.get(RPM_CHANGE_GROUP));
          adminJooq
              .insertInto(ROLE_PERMISSION_METADATA)
              .columns(
                  RPM_SCHEMA_NAME,
                  RPM_ROLE_NAME,
                  RPM_TABLE_NAME,
                  RPM_SELECT_SCOPE,
                  RPM_INSERT_SCOPE,
                  RPM_UPDATE_SCOPE,
                  RPM_DELETE_SCOPE,
                  RPM_CHANGE_OWNER,
                  RPM_CHANGE_GROUP)
              .values(
                  schemaName,
                  roleName,
                  tableName,
                  selectScope,
                  insertScope,
                  updateScope,
                  deleteScope,
                  changeOwner,
                  changeGroup)
              .onConflict(RPM_SCHEMA_NAME, RPM_ROLE_NAME, RPM_TABLE_NAME)
              .doUpdate()
              .set(RPM_SELECT_SCOPE, selectScope)
              .set(RPM_INSERT_SCOPE, insertScope)
              .set(RPM_UPDATE_SCOPE, updateScope)
              .set(RPM_DELETE_SCOPE, deleteScope)
              .execute();
        });
    database.getListener().onSchemaChange();
  }

  private static String mergeScope(String existing, Boolean granted, String noneValue) {
    if (Boolean.FALSE.equals(granted)) {
      return noneValue;
    }
    if (Boolean.TRUE.equals(granted)) {
      if (noneValue.equals(SelectScope.NONE.name())) {
        return SelectScope.ALL.name();
      }
      return UpdateScope.ALL.name();
    }
    return existing != null ? existing : noneValue;
  }

  public void revoke(String schemaName, String roleName, String tableName) {
    if (SYSTEM_ROLE_NAMES.contains(roleName)) {
      throw new MolgenisException("Cannot revoke permissions from system role: " + roleName);
    }
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .deleteFrom(ROLE_PERMISSION_METADATA)
                .where(
                    RPM_SCHEMA_NAME.eq(schemaName),
                    RPM_ROLE_NAME.eq(roleName),
                    RPM_TABLE_NAME.eq(tableName))
                .execute());
    database.getListener().onSchemaChange();
  }

  public void clearTablePermissionsForTable(String schemaName, String tableName) {
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .deleteFrom(ROLE_PERMISSION_METADATA)
                .where(RPM_SCHEMA_NAME.eq(schemaName), RPM_TABLE_NAME.eq(tableName))
                .execute());
  }

  public List<TablePermission> getPermissions(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      return systemPermissions(roleName);
    }
    List<TablePermission> result = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .select(
                    RPM_TABLE_NAME,
                    RPM_SELECT_SCOPE,
                    RPM_INSERT_SCOPE,
                    RPM_UPDATE_SCOPE,
                    RPM_DELETE_SCOPE)
                .from(ROLE_PERMISSION_METADATA)
                .where(
                    RPM_SCHEMA_NAME.eq(schemaName),
                    RPM_ROLE_NAME.eq(roleName),
                    RPM_TABLE_NAME.ne("*"),
                    RPM_TABLE_NAME.ne(RPM_STUB_TABLE_SENTINEL))
                .fetchStream()
                .forEach(
                    row -> {
                      Boolean canSelect =
                          !UpdateScope.NONE.name().equals(row.get(RPM_SELECT_SCOPE)) ? true : null;
                      Boolean canInsert =
                          !UpdateScope.NONE.name().equals(row.get(RPM_INSERT_SCOPE)) ? true : null;
                      Boolean canUpdate =
                          !UpdateScope.NONE.name().equals(row.get(RPM_UPDATE_SCOPE)) ? true : null;
                      Boolean canDelete =
                          !UpdateScope.NONE.name().equals(row.get(RPM_DELETE_SCOPE)) ? true : null;
                      result.add(
                          new TablePermission(row.get(RPM_TABLE_NAME))
                              .select(canSelect)
                              .insert(canInsert)
                              .update(canUpdate)
                              .delete(canDelete));
                    }));
    return result;
  }

  public Role getRole(String schemaName, String roleName) {
    boolean system = isSystemRole(roleName);
    return new Role(roleName, system, getPermissions(schemaName, roleName));
  }

  public List<Role> getRoles(String schemaName) {
    List<String> roleNames = new ArrayList<>(SYSTEM_ROLE_NAMES);
    roleNames.addAll(listRoles(schemaName));
    List<Role> result = new ArrayList<>();
    for (String roleName : roleNames) {
      result.add(getRole(schemaName, roleName));
    }
    return result;
  }

  public List<TablePermission> getTablePermissionsForActiveUser(String schemaName) {
    String activeUser = database.getActiveUser();
    List<String> tableNames = listTableNamesAsAdmin(schemaName);
    if (tableNames.isEmpty()) return List.of();
    SqlSchema schema = database.getSchema(schemaName);
    List<String> systemRoleNames =
        schema != null ? schema.getInheritedRolesForUser(activeUser) : List.of();
    List<String> customRoleNames = listCustomRolesForUser(schemaName, activeUser);
    if (systemRoleNames.isEmpty() && customRoleNames.isEmpty()) return List.of();
    Map<String, TablePermission> merged = new LinkedHashMap<>();
    for (String roleName : systemRoleNames) {
      List<TablePermission> perms =
          isSystemRole(roleName)
              ? systemPermissions(roleName)
              : getPermissions(schemaName, roleName);
      for (TablePermission perm : perms) {
        if (hasAnyPermission(perm)) {
          merged.merge(perm.table(), perm, SqlRoleManager::mergePermissions);
        }
      }
    }
    for (String roleName : customRoleNames) {
      for (TablePermission perm : getPermissions(schemaName, roleName)) {
        if (hasAnyPermission(perm)) {
          merged.merge(perm.table(), perm, SqlRoleManager::mergePermissions);
        }
      }
    }
    expandWildcard(merged, tableNames);
    return new ArrayList<>(merged.values());
  }

  private List<String> listTableNamesAsAdmin(String schemaName) {
    List<String> result = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .fetch(
                    "SELECT table_name FROM \"MOLGENIS\".table_metadata WHERE table_schema = ?",
                    schemaName)
                .stream()
                .map(row -> row.get("table_name", String.class))
                .forEach(result::add));
    return result;
  }

  private List<String> listCustomRolesForUser(String schemaName, String userName) {
    List<String> result = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq ->
            adminJooq
                .selectDistinct(GMM_ROLE_NAME)
                .from(GROUP_MEMBERSHIP_METADATA)
                .where(GMM_SCHEMA_NAME.eq(schemaName), GMM_USER_NAME.eq(userName))
                .fetchStream()
                .map(row -> row.get(GMM_ROLE_NAME))
                .filter(roleName -> !SYSTEM_ROLE_NAMES.contains(roleName))
                .forEach(result::add));
    return result;
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

  private static boolean hasAnyPermission(TablePermission perm) {
    return Boolean.TRUE.equals(perm.select())
        || Boolean.TRUE.equals(perm.insert())
        || Boolean.TRUE.equals(perm.update())
        || Boolean.TRUE.equals(perm.delete());
  }

  private static TablePermission mergePermissions(TablePermission a, TablePermission b) {
    return new TablePermission(a.table())
        .select(Boolean.TRUE.equals(a.select()) || Boolean.TRUE.equals(b.select()) ? true : null)
        .insert(Boolean.TRUE.equals(a.insert()) || Boolean.TRUE.equals(b.insert()) ? true : null)
        .update(Boolean.TRUE.equals(a.update()) || Boolean.TRUE.equals(b.update()) ? true : null)
        .delete(Boolean.TRUE.equals(a.delete()) || Boolean.TRUE.equals(b.delete()) ? true : null);
  }

  public void grantRoleToUser(Schema schema, String roleName, String username) {
    String schemaName = schema.getName();
    ensureDirectGrantGroupExists(schemaName);
    requireUserExists(username);
    insertGroupMembership(schemaName, DIRECT_GRANT_GROUP, username, roleName);
    grantMemberRoleToUser(schemaName, username);
    database.getListener().onSchemaChange();
  }

  public void revokeRoleFromUser(Schema schema, String roleName, String username) {
    String schemaName = schema.getName();
    deleteGroupMembership(schemaName, DIRECT_GRANT_GROUP, username, roleName);
    if (!userHasAnyGroupMembershipInSchema(schemaName, username)) {
      revokeMemberRoleFromUser(schemaName, username);
    }
    database.getListener().onSchemaChange();
  }

  private void ensureDirectGrantGroupExists(String schemaName) {
    database.getJooqAsAdmin(
        adminJooq -> {
          boolean exists =
              adminJooq.fetchExists(
                  adminJooq
                      .select()
                      .from(GROUPS_METADATA)
                      .where(GROUP_SCHEMA.eq(schemaName), GROUP_NAME.eq(DIRECT_GRANT_GROUP)));
          if (!exists) {
            adminJooq
                .insertInto(GROUPS_METADATA)
                .columns(GROUP_SCHEMA, GROUP_NAME)
                .values(schemaName, DIRECT_GRANT_GROUP)
                .execute();
          }
        });
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
        adminJooq ->
            found[0] =
                adminJooq.fetchExists(
                    adminJooq
                        .select()
                        .from(GROUP_MEMBERSHIP_METADATA)
                        .where(GMM_SCHEMA_NAME.eq(schemaName), GMM_USER_NAME.eq(activeUser))));
    return found[0];
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
    if (DIRECT_GRANT_GROUP.equals(groupName)) {
      throw new MolgenisException(
          "Group name '" + DIRECT_GRANT_GROUP + "' is reserved and cannot be used");
    }
    String schemaName = schema.getName();
    database.getJooqAsAdmin(
        adminJooq -> {
          Record existing =
              adminJooq.fetchOne(
                  "SELECT name FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
                  schemaName,
                  groupName);
          if (existing != null) {
            throw new MolgenisException(
                "Group '" + groupName + "' already exists in schema '" + schemaName + "'");
          }
          adminJooq.execute(
              "INSERT INTO \"MOLGENIS\".groups_metadata (schema, name) VALUES (?, ?)",
              schemaName,
              groupName);
        });
  }

  public void deleteGroup(Schema schema, String groupName) {
    if (DIRECT_GRANT_GROUP.equals(groupName)) {
      throw new MolgenisException("Reserved group name '__direct__' cannot be used");
    }
    String schemaName = schema.getName();
    database.getJooqAsAdmin(
        adminJooq -> {
          int deleted =
              adminJooq.execute(
                  "DELETE FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
                  schemaName,
                  groupName);
          if (deleted == 0) {
            throw new MolgenisException(
                "Group '" + groupName + "' not found in schema '" + schemaName + "'");
          }
        });
  }

  static final String GROUP_MEMBERSHIP_SENTINEL_ROLE = "member";

  public void addGroupMember(Schema schema, String groupName, String username) {
    addGroupMembership(schema.getName(), groupName, username, GROUP_MEMBERSHIP_SENTINEL_ROLE);
  }

  public void removeGroupMember(Schema schema, String groupName, String username) {
    removeGroupMembership(schema.getName(), groupName, username, GROUP_MEMBERSHIP_SENTINEL_ROLE);
  }

  public List<Map<String, Object>> listGroups(Schema schema) {
    String schemaName = schema.getName();
    List<Map<String, Object>> result = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq -> {
          Result<Record> rows =
              adminJooq.fetch(
                  "SELECT name FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name != ? ORDER BY name",
                  schemaName,
                  DIRECT_GRANT_GROUP);
          for (Record row : rows) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", row.get("name", String.class));
            List<Map<String, Object>> userRolePairs =
                findGroupUserRolePairs(adminJooq, schemaName, row.get("name", String.class));
            entry.put("users", userRolePairs);
            result.add(entry);
          }
        });
    return result;
  }

  public List<Map<String, Object>> listCustomMemberships(String schemaName) {
    List<Map<String, Object>> result = new ArrayList<>();
    database.getJooqAsAdmin(
        adminJooq -> {
          Result<Record> rows =
              adminJooq.fetch(
                  "SELECT user_name, role_name, group_name"
                      + " FROM \"MOLGENIS\".group_membership_metadata"
                      + " WHERE schema_name = ? AND role_name != ?",
                  schemaName,
                  GROUP_MEMBERSHIP_SENTINEL_ROLE);
          for (Record row : rows) {
            String groupValue = row.get("group_name", String.class);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("email", row.get("user_name", String.class));
            entry.put("role", row.get("role_name", String.class));
            entry.put("group", DIRECT_GRANT_GROUP.equals(groupValue) ? null : groupValue);
            result.add(entry);
          }
        });
    return result;
  }

  private List<Map<String, Object>> findGroupUserRolePairs(
      DSLContext adminJooq, String schemaName, String groupName) {
    return adminJooq
        .select(GMM_USER_NAME, GMM_ROLE_NAME)
        .from(GROUP_MEMBERSHIP_METADATA)
        .where(GMM_SCHEMA_NAME.eq(schemaName), GMM_GROUP_NAME.eq(groupName))
        .fetch()
        .stream()
        .map(
            r -> {
              Map<String, Object> pair = new LinkedHashMap<>();
              pair.put("name", r.get(GMM_USER_NAME));
              pair.put("role", r.get(GMM_ROLE_NAME));
              return (Map<String, Object>) pair;
            })
        .toList();
  }

  private void requireGroupExists(String schemaName, String groupName) {
    database.getJooqAsAdmin(
        adminJooq -> requireGroupExistsViaJooq(adminJooq, schemaName, groupName));
  }

  private void requireUserExists(String username) {
    database.getJooqAsAdmin(adminJooq -> requireUserExistsViaJooq(adminJooq, username));
  }

  private static void requireGroupExistsViaJooq(
      DSLContext adminJooq, String schemaName, String groupName) {
    Record existing =
        adminJooq.fetchOne(
            "SELECT name FROM \"MOLGENIS\".groups_metadata WHERE schema = ? AND name = ?",
            schemaName,
            groupName);
    if (existing == null) {
      throw new MolgenisException(
          "Group '" + groupName + "' not found in schema '" + schemaName + "'");
    }
  }

  private static void requireUserExistsViaJooq(DSLContext adminJooq, String username) {
    Record existing =
        adminJooq.fetchOne(
            "SELECT username FROM \"MOLGENIS\".users_metadata WHERE username = ?", username);
    if (existing == null) {
      throw new MolgenisException("User '" + username + "' does not exist");
    }
  }

  private static String sha1Hex(String input) {
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
    String memberRole = memberRoleName(schemaName);
    database.getJooqAsAdmin(
        adminJooq -> {
          adminJooq.execute(
              "ALTER TABLE {0} ADD COLUMN IF NOT EXISTS mg_owner TEXT DEFAULT current_user",
              name(schemaName, tableName));
          adminJooq.execute(
              "ALTER TABLE {0} ADD COLUMN IF NOT EXISTS mg_groups TEXT[]",
              name(schemaName, tableName));
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
          String changeTrigger = changeCapTriggerName(tableName);
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
          adminJooq.execute(
              "GRANT SELECT, INSERT, UPDATE, DELETE ON {0} TO {1}",
              name(schemaName, tableName), name(memberRole));
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
    String memberRole = memberRoleName(schemaName);
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
          String changeTrigger = changeCapTriggerName(tableName);
          adminJooq.execute(
              "DROP TRIGGER IF EXISTS {0} ON {1}",
              name(changeTrigger), name(schemaName, tableName));
          adminJooq.execute(
              "ALTER TABLE {0} DISABLE ROW LEVEL SECURITY", name(schemaName, tableName));
          adminJooq.execute(
              "REVOKE SELECT, INSERT, UPDATE, DELETE ON {0} FROM {1}",
              name(schemaName, tableName), name(memberRole));
        });
  }

  private static String tablePolicyName(String tableName, String verb) {
    String candidate = "mg_p_" + tableName + "_" + verb;
    if (candidate.getBytes(UTF_8).length <= PG_MAX_ID_LENGTH) {
      return candidate;
    }
    String hash = sha1Hex(tableName + "_" + verb).substring(0, 12);
    return "mg_p_" + hash;
  }

  private static String changeCapTriggerName(String tableName) {
    String candidate = "mg_check_change_cap_" + tableName;
    if (candidate.getBytes(UTF_8).length <= PG_MAX_ID_LENGTH) {
      return candidate;
    }
    String hash = sha1Hex(tableName).substring(0, 12);
    return "mg_check_change_cap_" + hash;
  }

  void rejectDisableIfPermissionsExist(String schemaName, String tableName) {
    boolean[] hasPermissions = {false};
    database.getJooqAsAdmin(
        adminJooq ->
            hasPermissions[0] =
                adminJooq.fetchExists(
                    adminJooq
                        .select()
                        .from(ROLE_PERMISSION_METADATA)
                        .where(RPM_SCHEMA_NAME.eq(schemaName), RPM_TABLE_NAME.eq(tableName))));
    if (hasPermissions[0]) {
      throw new MolgenisException(
          "Cannot disable RLS: first remove permissions on '" + schemaName + "." + tableName + "'");
    }
  }
}
