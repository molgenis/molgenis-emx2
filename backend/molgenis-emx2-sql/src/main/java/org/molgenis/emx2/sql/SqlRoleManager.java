package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;

import java.util.ArrayList;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SqlRoleManager {
  private static final Logger logger = LoggerFactory.getLogger(SqlRoleManager.class);

  public static final String PG_ROLES = "pg_roles";
  public static final String ROLNAME = "rolname";

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
    String existsRole = fullRoleName(schemaName, Privileges.EXISTS.toString());
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
      jooq()
          .execute(
              "REVOKE ALL ON {0} FROM {1}", table(name(schemaName, tableName)), name(fullRole));
    }
    jooq()
        .execute(
            """
                DO $$ DECLARE m TEXT; BEGIN
                 FOR m IN SELECT rolname FROM pg_roles
                 WHERE pg_has_role(rolname, {0}, 'member') AND rolname <> {0}
                 LOOP EXECUTE 'REVOKE ' || quote_ident({0}) || ' FROM ' || quote_ident(m);
                 END LOOP; END $$;""",
            inline(fullRole));
    jooq().execute("DROP ROLE IF EXISTS {0}", name(fullRole));
    database.getListener().schemaChanged(schemaName);
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
    database.getListener().schemaChanged(schemaName);
  }

  public void revoke(String schemaName, String roleName, String tableName) {
    String fullRole = fullRoleName(schemaName, roleName);
    jooq()
        .execute("REVOKE ALL ON {0} FROM {1}", table(name(schemaName, tableName)), name(fullRole));
    database.getListener().schemaChanged(schemaName);
  }

  private void applyPgGrants(
      String schemaName, String fullRole, String tableName, TablePermission p) {
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    if (p.select() != null) {
      jooq().execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
    }
    if (Boolean.TRUE.equals(p.insert())) {
      jooq().execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
    }
    if (Boolean.TRUE.equals(p.update())) {
      jooq().execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
    }
    if (Boolean.TRUE.equals(p.delete())) {
      jooq().execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
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
        Privileges select =
            Boolean.TRUE.equals(row.get("can_select", Boolean.class)) ? Privileges.VIEWER : null;
        Boolean insert = Boolean.TRUE.equals(row.get("can_insert", Boolean.class)) ? true : null;
        Boolean update = Boolean.TRUE.equals(row.get("can_update", Boolean.class)) ? true : null;
        Boolean delete = Boolean.TRUE.equals(row.get("can_delete", Boolean.class)) ? true : null;
        result.add(
            new TablePermission(
                row.get("table_name", String.class), select, insert, update, delete));
      }
    } catch (Exception e) {
      logger.error("Failed to get permissions for {} in {}", roleName, schemaName, e);
    }
    return result;
  }

  public Role getRole(String schemaName, String roleName) {
    boolean system = isSystemRole(roleName);
    String description = system ? null : getDescription(schemaName, roleName);
    return new Role(roleName, description, system, getPermissions(schemaName, roleName));
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

  public List<TablePermission> getPermissionsForActiveUser(String schemaName) {
    String activeUser = database.getActiveUser();
    if (activeUser == null || ANONYMOUS.equals(activeUser)) {
      return List.of();
    }
    SqlSchema schema = database.getSchema(schemaName);
    if (schema == null) {
      return List.of();
    }
    String roleName = schema.getRoleForUser(activeUser);
    if (roleName == null || roleName.isEmpty()) {
      return List.of();
    }
    return getPermissions(schemaName, roleName);
  }

  public boolean isSystemRole(String roleName) {
    return Privileges.isSystemRole(roleName);
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleName;
  }

  public void setDescription(String schemaName, String roleName, String description) {
    jooq()
        .execute(
            "COMMENT ON ROLE {0} IS {1}",
            name(fullRoleName(schemaName, roleName)), inline(description));
  }

  private String getDescription(String schemaName, String roleName) {
    return jooq()
        .select(field("shobj_description(oid, 'pg_authid')")) // TODO: do we need a description?
        .from(PG_ROLES)
        .where(field(ROLNAME).eq(inline(fullRoleName(schemaName, roleName))))
        .fetchOne(0, String.class);
  }

  private List<TablePermission> systemPermissions(String roleName) {
    return switch (roleName) {
      case "Exists" -> List.of(new TablePermission("*", Privileges.EXISTS, null, null, null));
      case "Range" -> List.of(new TablePermission("*", Privileges.RANGE, null, null, null));
      case "Aggregator" ->
          List.of(new TablePermission("*", Privileges.AGGREGATOR, null, null, null));
      case "Count" -> List.of(new TablePermission("*", Privileges.COUNT, null, null, null));
      case "Viewer" -> List.of(new TablePermission("*", Privileges.VIEWER, null, null, null));
      case "Editor", "Manager", "Owner" ->
          List.of(new TablePermission("*", Privileges.VIEWER, true, true, true));
      default -> List.of();
    };
  }
}
