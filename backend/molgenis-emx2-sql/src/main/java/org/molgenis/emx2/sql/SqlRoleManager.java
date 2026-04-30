package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;
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
    if (p.hasSelect()) {
      jooq().execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.select())) {
      jooq().execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.hasInsert()) {
      jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.insert())) {
      jooq().execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.hasUpdate()) {
      jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.update())) {
      jooq().execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.hasDelete()) {
      jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.delete())) {
      jooq().execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
  }

  public List<String> getRoleNames(String schemaName) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    return jooq()
        .select(field(ROLNAME))
        .from(PG_ROLES)
        .where(field(ROLNAME).like(inline(rolePrefix + "%")))
        .fetch(r -> r.get(ROLNAME, String.class).substring(rolePrefix.length()));
  }

  public void addMember(String schemaName, Member member) {
    List<String> currentRoles = getRoleNames(schemaName);
    if (!currentRoles.contains(member.getRole())) {
      throw new MolgenisException(
          "Add member(s) failed: Role '"
              + member.getRole()
              + "' doesn't exist in schema '"
              + schemaName
              + "'. Existing roles are: "
              + currentRoles);
    }
    database.tx(
        db -> {
          SqlDatabase txDb = (SqlDatabase) db;
          List<Member> currentMembers = getMembers(txDb.getJooq(), schemaName);
          String username = MG_USER_PREFIX + member.getUser();
          String roleName = MG_ROLE_PREFIX + schemaName + "/" + member.getRole();
          if (!db.hasUser(member.getUser())) {
            db.addUser(member.getUser());
          }
          for (Member old : currentMembers) {
            if (old.getUser().equals(member.getUser())) {
              txDb.getJooq()
                  .execute(
                      "REVOKE {0} FROM {1}",
                      name(MG_ROLE_PREFIX + schemaName + "/" + old.getRole()), name(username));
            }
          }
          try {
            txDb.getJooq().execute("GRANT {0} TO {1}", name(roleName), name(username));
          } catch (DataAccessException dae) {
            throw new SqlMolgenisException("Add member failed", dae);
          }
        });
  }

  public List<Member> getMembers(String schemaName) {
    return getMembers(jooq(), schemaName);
  }

  private List<Member> getMembers(DSLContext jooq, String schemaName) {
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    String userPrefix = MG_USER_PREFIX;
    List<Member> members = new ArrayList<>();
    List<Record> result =
        jooq.fetch(
            "select distinct m.rolname as member, r.rolname as role"
                + " from pg_catalog.pg_auth_members am "
                + " join pg_catalog.pg_roles m on (m.oid = am.member)"
                + " join pg_catalog.pg_roles r on (r.oid = am.roleid)"
                + " where r.rolname LIKE {0} and m.rolname LIKE {1}",
            rolePrefix + "%", userPrefix + "%");
    for (Record r : result) {
      String memberName = r.getValue("member", String.class).substring(userPrefix.length());
      String roleName = r.getValue("role", String.class).substring(rolePrefix.length());
      members.add(new Member(memberName, roleName));
    }
    return members;
  }

  public void removeMembers(String schemaName, List<Member> members) {
    List<String> usernames = members.stream().map(Member::getUser).toList();
    String userPrefix = MG_USER_PREFIX;
    String rolePrefix = MG_ROLE_PREFIX + schemaName + "/";
    try {
      for (Member m : getMembers(schemaName)) {
        if (usernames.contains(m.getUser())) {
          jooq()
              .execute(
                  "REVOKE {0} FROM {1}",
                  name(rolePrefix + m.getRole()), name(userPrefix + m.getUser()));
        }
      }
    } catch (DataAccessException dae) {
      throw new SqlMolgenisException("Remove of member failed", dae);
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
        result.add(
            new TablePermission(row.get("table_name", String.class))
                .select(trueOrNull(row, "can_select"))
                .insert(trueOrNull(row, "can_insert"))
                .update(trueOrNull(row, "can_update"))
                .delete(trueOrNull(row, "can_delete")));
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
    return getRoleNames(schemaName).stream().map(name -> getRole(schemaName, name)).toList();
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
    return p.hasSelect() || p.hasInsert() || p.hasUpdate() || p.hasDelete();
  }

  private static TablePermission mergePermissions(TablePermission a, TablePermission b) {
    return new TablePermission(a.table())
        .select(a.hasSelect() || b.hasSelect() ? true : null)
        .insert(a.hasInsert() || b.hasInsert() ? true : null)
        .update(a.hasUpdate() || b.hasUpdate() ? true : null)
        .delete(a.hasDelete() || b.hasDelete() ? true : null);
  }

  private static Boolean trueOrNull(Record row, String field) {
    return Boolean.TRUE.equals(row.get(field, Boolean.class)) ? true : null;
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
