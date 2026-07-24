package org.molgenis.emx2.sql;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING_ARRAY;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.sql.MetadataUtils.*;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.executeCreateRole;
import static org.molgenis.emx2.sql.SqlRoleManager.RlsPolicy.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.exception.DataAccessException;
import org.molgenis.emx2.*;
import org.molgenis.emx2.Role;

public class SqlRoleManager {

  public static final String PG_ROLES = "pg_roles";
  public static final String ROLNAME = "rolname";
  public static final int PG_MAX_ID_LENGTH = 63;
  public static final String RLS_ROLE_PREFIX = "RLS_";

  enum RlsPolicy {
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
    validateRoleName(schemaName, roleName);
    createRoleWithGrants(schemaName, roleName);
  }

  private void createRoleWithGrants(String schemaName, String roleName) {
    String fullRole = fullRoleName(schemaName, roleName);
    String existsRole = fullRoleName(schemaName, Privileges.EXISTS.toString());
    String ownerRole = fullRoleName(schemaName, Privileges.OWNER.toString());
    database.tx( // we need to lift to admin to create a role
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            DSLContext jooq = ((SqlDatabase) db).getJooq();
            executeCreateRole(jooq, fullRole);
            grantWithAdminOption(jooq, name(fullRole), keyword("session_user"));
            grantWithAdminOption(jooq, name(fullRole), name(ownerRole));
            grant(jooq, name(existsRole), name(fullRole));
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
            assertNoMgRolesReference(jooq, schemaName, roleName, tableNames);

            deleteRoleAndGrants(jooq, schemaName, fullRole, tableNames);
            if (roleExists(rlsFullRole)) {
              deleteRoleAndGrants(jooq, schemaName, rlsFullRole, tableNames);
            }
          } finally {
            db.setActiveUser(currentUser);
          }
        });
    database.getListener().onSchemaChange();
  }

  public boolean roleExists(String fullRoleName) {
    return jooq()
        .fetchExists(jooq().select().from(PG_ROLES).where(field(ROLNAME).eq(inline(fullRoleName))));
  }

  public boolean roleExists(String schemaName, String roleName) {
    return roleExists(fullRoleName(schemaName, roleName));
  }

  private void validateRoleName(String schemaName, String roleName) {
    if (isSystemRole(roleName)) {
      throw new MolgenisException("Cannot create system role: " + roleName);
    }
    if (roleName.startsWith(RLS_ROLE_PREFIX)) {
      throw new MolgenisException(
          "Cannot create role '"
              + roleName
              + "': the '"
              + RLS_ROLE_PREFIX
              + "' prefix is reserved for internal row-level-security roles.");
    }
    int rlsOverhead = RLS_ROLE_PREFIX.getBytes(UTF_8).length;
    if (fullRoleName(schemaName, roleName).getBytes(UTF_8).length + rlsOverhead
        > PG_MAX_ID_LENGTH) {
      throw new MolgenisException(
          "Role name '" + roleName + "' is too long, it exceeds PostgreSQL's 63-byte limit");
    }
  }

  private void assertNoMgRolesReference(
      DSLContext jooq, String schemaName, String roleName, Collection<String> tableNames) {
    SchemaMetadata schemaMetadata = database.getSchema(schemaName).getMetadata();
    for (String tableName : tableNames) {
      if (schemaMetadata.getTableMetadata(tableName).getColumn(MG_ROLES) == null) {
        continue;
      }
      int count =
          jooq.fetchCount(
              jooq.selectOne()
                  .from(table(name(schemaName, tableName)))
                  .where("{0} = ANY(mg_roles)", inline(roleName)));
      if (count > 0) {
        throw new MolgenisException(
            "Cannot delete role '"
                + roleName
                + "': "
                + count
                + " row(s) in table '"
                + tableName
                + "' still reference it in mg_roles");
      }
    }
  }

  private static void deleteRoleAndGrants(
      DSLContext jooq, String schemaName, String fullRole, Collection<String> tableNames) {
    for (String tableName : tableNames) {
      revokeAll(jooq, table(name(schemaName, tableName)), name(fullRole));
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
    boolean isRowLevel = permission.hasRowLevel();
    String grantRoleName = isRowLevel ? RLS_ROLE_PREFIX + roleName : roleName;
    if (isRowLevel) {
      createRlsRole(schemaName, roleName);
      if (tableMetadata.getColumn(MG_ROLES) == null) {
        tableMetadata.add(column(MG_ROLES).setType(STRING_ARRAY));
      }
    }
    String fullRole = fullRoleName(schemaName, grantRoleName);
    database.tx(
        db -> {
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          for (TableMetadata tableInTree : tableMetadata.getInheritanceTree()) {
            applyPgGrants(jooq, schemaName, fullRole, tableInTree.getTableName(), permission);
          }
          if (isRowLevel) {
            enableRowLevelSecurityOnTree(jooq, schemaName, tableMetadata);
          }
        });
    database.getListener().onSchemaChange();
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
    database.tx(
        db -> {
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          boolean rlsRoleExists =
              jooq.fetchExists(
                  jooq.select().from(PG_ROLES).where(field(ROLNAME).eq(inline(rlsFullRole))));
          for (TableMetadata tableInTree : tableMetadata.getInheritanceTree()) {
            org.jooq.Table<?> jooqTable = table(name(schemaName, tableInTree.getTableName()));
            revokeAll(jooq, jooqTable, name(fullRole));
            if (rlsRoleExists) {
              revokeAll(jooq, jooqTable, name(rlsFullRole));
            }
          }
          disableRowLevelSecurityIfUnused(jooq, schemaName, tableMetadata);
        });
    database.getListener().onSchemaChange();
  }

  private TableMetadata requireRootTable(String schemaName, String tableName) {
    if (!database.getSchema(schemaName).getTableNames().contains(tableName)) {
      throw new MolgenisException("Table does not exist: " + tableName);
    }
    TableMetadata meta = database.getSchema(schemaName).getMetadata().getTableMetadata(tableName);
    if (!meta.getInheritNames().isEmpty()) {
      throw new MolgenisException(
          "Cannot grant custom permission on inherited table '"
              + tableName
              + "': grant on the root table '"
              + meta.getRootTable().getTableName()
              + "' instead. Permissions on root tables propagate to all subclasses.");
    }
    return meta;
  }

  private static void applyPgGrants(
      DSLContext jooq, String schemaName, String fullRole, String tableName, TablePermission p) {
    org.jooq.Table<?> jooqTable = table(name(schemaName, tableName));
    if (p.hasSelect()) {
      jooq.execute("GRANT SELECT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.select())) {
      jooq.execute("REVOKE SELECT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.hasInsert()) {
      jooq.execute("GRANT INSERT ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.insert())) {
      jooq.execute("REVOKE INSERT ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.hasUpdate()) {
      jooq.execute("GRANT UPDATE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.update())) {
      jooq.execute("REVOKE UPDATE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
    if (p.hasDelete()) {
      jooq.execute("GRANT DELETE ON {0} TO {1}", jooqTable, name(fullRole));
    } else if (Boolean.FALSE.equals(p.delete())) {
      jooq.execute("REVOKE DELETE ON {0} FROM {1}", jooqTable, name(fullRole));
    }
  }

  private void createRlsRole(String schemaName, String roleName) {
    String rlsRoleName = RLS_ROLE_PREFIX + roleName;
    if (!roleExists(schemaName, rlsRoleName)) {
      createRoleWithGrants(schemaName, rlsRoleName);
    }
    String rlsFullRole = fullRoleName(schemaName, rlsRoleName);
    String regularFullRole = fullRoleName(schemaName, roleName);
    database.tx(
        db -> {
          String currentUser = db.getActiveUser();
          try {
            db.becomeAdmin();
            grant(((SqlDatabase) db).getJooq(), name(rlsFullRole), name(regularFullRole));
          } finally {
            db.setActiveUser(currentUser);
          }
        });
  }

  private void enableRowLevelSecurityOnTree(
      DSLContext jooq, String schemaName, TableMetadata root) {
    enableRowLevelSecurityOnTable(jooq, schemaName, root.getTableName(), rowRoleMatch(schemaName));
    for (TableMetadata subclass : root.getSubclassTables()) {
      enableRowLevelSecurityOnTable(
          jooq,
          schemaName,
          subclass.getTableName(),
          rowRoleMatchViaRoot(schemaName, root, subclass));
    }
  }

  private void enableRowLevelSecurityOnTable(
      DSLContext jooq, String schemaName, String tableName, Condition rowMatchCondition) {
    org.jooq.Table<?> table = table(name(schemaName, tableName));
    jooq.execute("ALTER TABLE {0} ENABLE ROW LEVEL SECURITY", table);
    dropRlsPolicies(jooq, table);

    createPolicy(jooq, table, VIEWER_BYPASS, hasSystemRoleMember(schemaName, Privileges.VIEWER));
    createPolicy(jooq, table, EDITOR_BYPASS, hasSystemRoleMember(schemaName, Privileges.EDITOR));
    createPolicy(jooq, table, TABLE_GRANT_BYPASS, tableGrantBypass(schemaName, tableName));
    createPolicy(jooq, table, ROW_MATCH, rowMatchCondition);
  }

  private static void createPolicy(
      DSLContext jooq, org.jooq.Table<?> jooqTable, RlsPolicy policy, Condition condition) {
    String expression = jooq.render(condition);
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
    jooq.execute(sql, jooqTable);
  }

  static void dropRlsPolicies(DSLContext jooq, org.jooq.Table<?> jooqTable) {
    for (RlsPolicy policy : RlsPolicy.values()) {
      jooq.execute("DROP POLICY IF EXISTS " + policy.policyName + " ON {0}", jooqTable);
    }
  }

  private static void disableRowLevelSecurityIfUnused(
      DSLContext jooq, String schemaName, TableMetadata root) {
    String rlsRolePrefix = rolePrefix(schemaName) + RLS_ROLE_PREFIX;
    boolean anyRlsGrantRemains =
        jooq.fetchExists(
            jooq.select()
                .from("information_schema.role_table_grants")
                .where(TABLE_SCHEMA.eq(inline(schemaName)))
                .and(TABLE_NAME.eq(inline(root.getTableName())))
                .and(field("grantee").like(inline(rlsRolePrefix + "%"))));
    if (anyRlsGrantRemains) return;
    for (TableMetadata tableInTree : root.getInheritanceTree()) {
      org.jooq.Table<?> jooqTable = table(name(schemaName, tableInTree.getTableName()));
      dropRlsPolicies(jooq, jooqTable);
      jooq.execute("ALTER TABLE {0} DISABLE ROW LEVEL SECURITY", jooqTable);
    }
  }

  private Condition hasSystemRoleMember(String schemaName, Privileges role) {
    return condition(
        "pg_has_role(current_user, {0}, 'member')",
        inline(fullRoleName(schemaName, role.toString())));
  }

  private Condition tableGrantBypass(String schemaName, String tableName) {
    String rolePrefix = rolePrefix(schemaName);
    String rlsRolePrefix = rolePrefix + RLS_ROLE_PREFIX;

    Field<Object> accessControlList =
        field(
            select(field("relacl"))
                .from(table("pg_class"))
                .where(
                    field("oid").eq(field("{0}::regclass", inline(name(schemaName, tableName))))));

    List<Param<String>> systemRoles =
        Arrays.stream(Privileges.values())
            .map(p -> inline(fullRoleName(schemaName, p.toString())))
            .toList();

    Field<String> rolname = field(name("r", ROLNAME), String.class);

    return exists(
        selectOne()
            .from(table("aclexplode({0})", accessControlList).as("ace"))
            .join(table(PG_ROLES).as("r"))
            .on(field(name("r", "oid")).eq(field(name("ace", "grantee"))))
            .where(rolname.like(inline(rolePrefix + "%")))
            .and(rolname.notLike(inline(rlsRolePrefix + "%")))
            .and(rolname.notIn(systemRoles))
            .and(condition("pg_has_role(current_user, {0}, 'member')", field(name("r", "oid")))));
  }

  private Condition rowRoleMatch(String schemaName) {
    String rolePrefix = rolePrefix(schemaName);
    return exists(
        selectOne()
            .from(table("unnest(mg_roles)").as("role_name"))
            .where(
                condition(
                    "pg_has_role(current_user, {0} || role_name, 'member')", inline(rolePrefix))));
  }

  private Condition rowRoleMatchViaRoot(
      String schemaName, TableMetadata root, TableMetadata subclass) {
    String rolePrefix = rolePrefix(schemaName);

    Condition pkeyJoin =
        and(
            subclass.getPrimaryKeyFields().stream()
                .map(
                    pkeyField ->
                        field(name("root_row", pkeyField.getName()))
                            .eq(field(name(pkeyField.getName()))))
                .toList());

    return exists(
        selectOne()
            .from(
                table(name(schemaName, root.getTableName())).as("root_row"),
                table("unnest(root_row.mg_roles)").as("role_name"))
            .where(pkeyJoin)
            .and(
                condition(
                    "pg_has_role(current_user, {0} || role_name, 'member')", inline(rolePrefix))));
  }

  public void addMember(String schemaName, Member member) {
    if (member.getRole().startsWith(RLS_ROLE_PREFIX)) {
      throw new MolgenisException(
          "Add member(s) failed: Role '"
              + member.getRole()
              + "' is an internal row-level-security role and cannot be assigned directly.");
    }
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
          String roleName = fullRoleName(schemaName, member.getRole());
          if (!db.hasUser(member.getUser())) {
            db.addUser(member.getUser());
          }
          boolean newRoleIsSystemRole = Privileges.isSystemRole(member.getRole());
          for (Member old : currentMembers) {
            if (old.getUser().equals(member.getUser())
                && Privileges.isSystemRole(old.getRole()) == newRoleIsSystemRole) {
              revoke(txDb.getJooq(), name(fullRoleName(schemaName, old.getRole())), name(username));
            }
          }
          try {
            grant(txDb.getJooq(), name(roleName), name(username));
          } catch (DataAccessException dae) {
            throw new SqlMolgenisException("Add member failed", dae);
          }
        });
  }

  public void removeMembers(String schemaName, List<Member> members) {
    List<String> usernames = members.stream().map(Member::getUser).toList();
    String rolePrefix = rolePrefix(schemaName);
    database.tx(
        db -> {
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          try {
            for (Member m : getMembers(jooq, schemaName)) {
              if (usernames.contains(m.getUser())) {
                revoke(jooq, name(rolePrefix + m.getRole()), name(MG_USER_PREFIX + m.getUser()));
              }
            }
          } catch (DataAccessException dae) {
            throw new SqlMolgenisException("Remove of member failed", dae);
          }
        });
  }

  public List<Member> getMembers(String schemaName) {
    return getMembers(jooq(), schemaName);
  }

  private List<Member> getMembers(DSLContext jooq, String schemaName) {
    Field<String> memberRolname = field(name("m", ROLNAME), String.class);
    Field<String> roleRolname = field(name("r", ROLNAME), String.class);
    Field<String> memberField = memberRolname.as("member");
    Field<String> roleField = roleRolname.as("role");

    return jooq.selectDistinct(memberField, roleField)
        .from(PG_AUTH_MEMBERS.as("am"))
        .join(PG_CATALOG_ROLES.as("m"))
        .on(field(name("m", "oid")).eq(field(name("am", "member"))))
        .join(PG_CATALOG_ROLES.as("r"))
        .on(field(name("r", "oid")).eq(field(name("am", "roleid"))))
        .where(roleRolname.like(inline(rolePrefix(schemaName) + "%")))
        .and(memberRolname.like(inline(MG_USER_PREFIX + "%")))
        .fetch(
            r ->
                new Member(
                    r.get(memberField).substring(MG_USER_PREFIX.length()),
                    r.get(roleField).substring(rolePrefix(schemaName).length())));
  }

  public List<String> getRoleNames(String schemaName) {
    String rolePrefix = rolePrefix(schemaName);
    return jooq()
        .select(field(ROLNAME))
        .from(PG_ROLES)
        .where(field(ROLNAME).like(inline(rolePrefix + "%")))
        .fetch(r -> r.get(ROLNAME, String.class).substring(rolePrefix.length()));
  }

  public List<Role> getRoles(String schemaName) {
    return getRoleNames(schemaName).stream()
        .filter(name -> !name.startsWith(RLS_ROLE_PREFIX))
        .map(name -> getRole(schemaName, name))
        .toList();
  }

  public Role getRole(String schemaName, String roleName) {
    boolean system = isSystemRole(roleName);
    List<TablePermission> permissions = getPermissions(schemaName, roleName);
    if (!system) {
      Set<String> rootTables =
          database.getSchema(schemaName).getMetadata().getRootTables().stream()
              .map(TableMetadata::getTableName)
              .collect(Collectors.toSet());
      permissions =
          permissions.stream()
              .filter(permission -> rootTables.contains(permission.table()))
              .toList();
    }
    return new Role(roleName, system, permissions);
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
        result.add(
            new TablePermission(row.get("table_name", String.class))
                .select(grantedOrNull(row, "can_select"))
                .insert(grantedOrNull(row, "can_insert"))
                .update(grantedOrNull(row, "can_update"))
                .delete(grantedOrNull(row, "can_delete"))
                .rowLevel(grantedOrNull(row, "is_row_level")));
      }
    } catch (Exception e) {
      throw new SqlMolgenisException("Failed to get permissions for " + roleName, e);
    }
    return result;
  }

  public List<TablePermission> getTablePermissionsForActiveUser(String schemaName) {
    SqlSchema schema = database.getSchema(schemaName);
    List<String> roleNames = schema.getInheritedRolesForActiveUser();

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
            .filter(p -> "*".equals(p.table()) && p.hasAny())
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
          .filter(TablePermission::hasAny)
          .forEach(p -> result.put(p.table(), p));
    }

    if (systemWildcard != null) {
      boolean systemHasDml = systemWildcard.hasModify();
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
          if (tp.hasAny()) result.put(tableName, tp);
        }
      }
    }

    return new ArrayList<>(result.values());
  }

  /**
   * Reads a granted-privilege flag, normalizing FALSE to null. TablePermission is tri-state (true =
   * grant, false = explicit revoke, null = absent), so an absent grant must be null and never false
   * — otherwise the permission could round-trip into applyPgGrants as an accidental REVOKE.
   */
  private static Boolean grantedOrNull(Record row, String field) {
    return Boolean.TRUE.equals(row.get(field, Boolean.class)) ? Boolean.TRUE : null;
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

  private static void grantWithAdminOption(DSLContext jooq, QueryPart role, QueryPart toRole) {
    jooq.execute("GRANT {0} TO {1} WITH ADMIN OPTION", role, toRole);
  }

  private static void grant(DSLContext jooq, QueryPart role, QueryPart toRole) {
    jooq.execute("GRANT {0} TO {1}", role, toRole);
  }

  private static void revokeAll(DSLContext jooq, Table<?> table, Name fromRole) {
    jooq.execute("REVOKE ALL ON {0} FROM {1}", table, fromRole);
  }

  private static void revoke(DSLContext jooq, Name role, Name fromRole) {
    jooq.execute("REVOKE {0} FROM {1}", role, fromRole);
  }

  public boolean isSystemRole(String roleName) {
    return Privileges.isSystemRole(roleName);
  }

  public static String rolePrefix(String schemaName) {
    return MG_ROLE_PREFIX + schemaName + "/";
  }

  public static String fullRoleName(String schemaName, String roleName) {
    return rolePrefix(schemaName) + roleName;
  }

  private static Boolean orBool(Boolean a, Boolean b) {
    return Boolean.TRUE.equals(a) || Boolean.TRUE.equals(b) ? true : null;
  }
}
