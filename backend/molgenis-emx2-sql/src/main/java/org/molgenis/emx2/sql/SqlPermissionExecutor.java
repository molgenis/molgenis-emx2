package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.MG_ROLE_PREFIX;
import static org.molgenis.emx2.Constants.MG_USER_PREFIX;
import static org.molgenis.emx2.sql.MetadataUtils.*;

import java.util.List;
import org.jooq.DSLContext;
import org.molgenis.emx2.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages PostgreSQL roles, grants, and row-level security policies directly from Java.
 *
 * <p>This replaces the trigger-based approach (group_metadata_trigger_function,
 * group_permissions_trigger_function, create_or_update_schema_groups) with explicit, debuggable
 * Java methods that follow the existing executor pattern.
 *
 * <p>The metadata tables (group_metadata, group_permissions) remain as passive storage. This class
 * performs both the metadata write AND the corresponding DDL side effects.
 */
class SqlPermissionExecutor {

  private static final Logger logger = LoggerFactory.getLogger(SqlPermissionExecutor.class);

  private SqlPermissionExecutor() {
    // hide constructor — static utility class
  }

  // ── Schema group creation ──────────────────────────────────────────────────

  /**
   * Creates the default role hierarchy for a schema. Replaces the PL/pgSQL function
   * create_or_update_schema_groups(schema_id).
   *
   * <p>Creates roles: Exists, Range, Aggregator, Count, Viewer, Editor, Manager, Owner, Admin and
   * establishes the inheritance chain between them.
   */
  static void createSchemaGroups(DSLContext jooq, String schemaName) {
    // Ensure global ADMIN group exists
    ensureGlobalAdmin(jooq, schemaName);

    // Create each role in the hierarchy with its permissions
    createGroupWithPermissions(jooq, schemaName, "Exists", true, false, false, false, false);
    createGroupWithPermissions(jooq, schemaName, "Range", true, false, false, false, false);
    createGroupWithPermissions(jooq, schemaName, "Aggregator", true, false, false, false, false);
    createGroupWithPermissions(jooq, schemaName, "Count", true, false, false, false, false);
    createGroupWithPermissions(jooq, schemaName, "Viewer", true, false, false, false, false);
    createGroupWithPermissions(jooq, schemaName, "Editor", true, true, true, true, false);
    createGroupWithPermissions(jooq, schemaName, "Manager", true, true, true, true, false);
    createGroupWithPermissions(jooq, schemaName, "Owner", true, true, true, true, false);
    createGroupWithPermissions(jooq, schemaName, "Admin", true, true, true, true, true);

    // Establish role inheritance chain
    grantRoleToRole(jooq, schemaName, "Exists", schemaName, "Range");
    grantRoleToRole(jooq, schemaName, "Range", schemaName, "Aggregator");
    grantRoleToRole(jooq, schemaName, "Aggregator", schemaName, "Count");
    grantRoleToRole(jooq, schemaName, "Count", schemaName, "Viewer");
    grantRoleToRole(jooq, schemaName, "Viewer", schemaName, "Editor");

    // Manager gets Editor, Viewer, Aggregator WITH ADMIN OPTION
    String managerRole = roleName(schemaName, "Manager");
    String editorRole = roleName(schemaName, "Editor");
    String viewerRole = roleName(schemaName, "Viewer");
    String aggregatorRole = roleName(schemaName, "Aggregator");
    jooq.execute(
        "GRANT {0}, {1}, {2} TO {3} WITH ADMIN OPTION",
        name(editorRole), name(viewerRole), name(aggregatorRole), name(managerRole));

    // Manager gets ALL on schema (for CREATE TABLE etc)
    jooq.execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schemaName), name(managerRole));

    // Owner gets Manager, Editor, Viewer, Aggregator WITH ADMIN OPTION
    String ownerRole = roleName(schemaName, "Owner");
    jooq.execute(
        "GRANT {0}, {1}, {2}, {3} TO {4} WITH ADMIN OPTION",
        name(editorRole),
        name(viewerRole),
        name(aggregatorRole),
        name(managerRole),
        name(ownerRole));

    // Admin inherits Owner
    grantRoleToRole(jooq, schemaName, "Owner", schemaName, "Admin");

    refreshPermissionsMaterializedView(jooq);

    logger.info("Created default permission groups for schema '{}'", schemaName);
  }

  // ── User-to-group management ───────────────────────────────────────────────

  /**
   * Adds a user to a group. Replaces the INSERT branch of group_metadata_trigger_function.
   *
   * <p>1. Updates the group_metadata.users array 2. Ensures the MG_USER_ role exists 3. GRANTs the
   * group role to the user role
   */
  static void addUserToGroup(DSLContext jooq, String groupName, String username) {
    String pgRole = MG_ROLE_PREFIX + groupName;
    String pgUser = MG_USER_PREFIX + username;

    // Update metadata: append user if not already present
    jooq.update(GROUP_METADATA)
        .set(
            USERS,
            when(not(condition("? = any(" + USERS + ")", username)), arrayAppend(USERS, username))
                .otherwise(USERS))
        .where(GROUP_NAME.eq(groupName))
        .execute();

    // Ensure user role exists
    ensureRoleExists(jooq, pgUser);

    // Grant group role to user
    jooq.execute("GRANT {0} TO {1}", name(pgRole), name(pgUser));

    refreshPermissionsMaterializedView(jooq);

    logger.debug("Added user '{}' to group '{}'", username, groupName);
  }

  /**
   * Removes a user from a group. Replaces the DELETE/UPDATE branch of
   * group_metadata_trigger_function.
   *
   * <p>1. Removes the user from group_metadata.users array 2. REVOKEs the group role from the user
   * role
   */
  static void removeUserFromGroup(DSLContext jooq, String groupName, String username) {
    String pgRole = MG_ROLE_PREFIX + groupName;
    String pgUser = MG_USER_PREFIX + username;

    // Update metadata: remove user from array
    jooq.update(GROUP_METADATA)
        .set(USERS, arrayRemove(USERS, username))
        .where(GROUP_NAME.eq(groupName))
        .execute();

    // Revoke group role from user
    jooq.execute("REVOKE {0} FROM {1}", name(pgRole), name(pgUser));

    refreshPermissionsMaterializedView(jooq);

    logger.debug("Removed user '{}' from group '{}'", username, groupName);
  }

  // ── Table permission grants ────────────────────────────────────────────────

  /**
   * Grants table-level permissions for a group. Replaces the INSERT branch of
   * group_permissions_trigger_function + grant_table_permissions().
   *
   * <p>If permission.tableId() is null, grants apply to ALL tables in the schema.
   */
  static void grantPermissionsForGroup(DSLContext jooq, String groupName, Permission permission) {
    String pgRole = MG_ROLE_PREFIX + groupName;

    // Save metadata
    savePermissions(jooq, groupName, permission);

    // Grant USAGE on schema
    jooq.execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(permission.tableSchema()), name(pgRole));

    if (permission.tableId() == null) {
      // Schema-level: grant to all existing tables
      List<String> tableNames = getTableNamesInSchema(jooq, permission.tableSchema());
      for (String tableName : tableNames) {
        grantTablePermissions(jooq, permission, pgRole, permission.tableSchema(), tableName);
      }
    } else {
      grantTablePermissions(
          jooq, permission, pgRole, permission.tableSchema(), permission.tableId());
    }
  }

  /**
   * Revokes table-level permissions for a group. Replaces the DELETE branch of
   * group_permissions_trigger_function + revoke_table_permissions().
   */
  static void revokePermissionsForGroup(DSLContext jooq, String groupName, Permission permission) {
    String pgRole = MG_ROLE_PREFIX + groupName;

    if (permission.tableId() == null) {
      List<String> tableNames = getTableNamesInSchema(jooq, permission.tableSchema());
      for (String tableName : tableNames) {
        revokeTablePermissions(jooq, permission, pgRole, permission.tableSchema(), tableName);
      }
    } else {
      revokeTablePermissions(
          jooq, permission, pgRole, permission.tableSchema(), permission.tableId());
    }

    // Revoke USAGE if no remaining permissions for this group in this schema
    boolean hasRemaining =
        jooq.fetchExists(
            selectOne()
                .from(GROUP_PERMISSIONS)
                .where(
                    GROUP_NAME.eq(groupName),
                    field(name("table_schema")).eq(permission.tableSchema())));

    if (!hasRemaining) {
      jooq.execute(
          "REVOKE USAGE ON SCHEMA {0} FROM {1}", name(permission.tableSchema()), name(pgRole));
    }
  }

  /**
   * Grants permissions on a newly created table to all schema-level groups. Called when a new table
   * is created in a schema that already has groups. Replaces the INSERT branch of
   * table_metadata_trigger_function.
   */
  static void grantPermissionsForNewTable(DSLContext jooq, String schemaName, String tableName) {
    // Find all schema-level permission entries (table_name IS NULL)
    var records =
        jooq.select(GROUP_NAME, HAS_SELECT, HAS_INSERT, HAS_UPDATE, HAS_DELETE, HAS_ADMIN)
            .from(GROUP_PERMISSIONS)
            .where(field(name("table_schema")).eq(schemaName), field(name("table_name")).isNull())
            .fetch();

    for (var record : records) {
      String groupName = record.get(GROUP_NAME);
      String pgRole = MG_ROLE_PREFIX + groupName;
      Permission perm =
          new Permission(
              null,
              schemaName,
              false,
              record.get(HAS_SELECT),
              record.get(HAS_INSERT),
              record.get(HAS_UPDATE),
              record.get(HAS_DELETE),
              record.get(HAS_ADMIN));
      grantTablePermissions(jooq, perm, pgRole, schemaName, tableName);
    }
  }

  // ── Row-Level Security ─────────────────────────────────────────────────────

  /**
   * Enables Row-Level Security on a table. Replaces the PL/pgSQL function enable_RLS_on_table().
   *
   * <p>Creates the mg_group column and four policies (SELECT, INSERT, UPDATE, DELETE).
   */
  static void enableRowLevelSecurity(DSLContext jooq, String schemaName, String tableName) {
    String safeSchema = schemaName.replace(" ", "_");
    String safeTable = tableName.replace(" ", "_");

    // Add mg_group column if not exists
    jooq.execute(
        "ALTER TABLE {0}.{1} ADD COLUMN IF NOT EXISTS mg_group TEXT[] DEFAULT NULL",
        name(schemaName), name(tableName));

    // Add column to metadata
    jooq.execute(
        "INSERT INTO {0} (table_schema, table_name, column_name, \"columnType\", key, position, cascade, readonly)"
            + " VALUES ({1}, {2}, 'mg_group', 'STRING_ARRAY', 0, -5, false, false)"
            + " ON CONFLICT DO NOTHING",
        table(name("MOLGENIS", "column_metadata")), val(schemaName), val(tableName));

    // Enable RLS
    jooq.execute(
        "ALTER TABLE {0}.{1} ENABLE ROW LEVEL SECURITY", name(schemaName), name(tableName));

    // Create SELECT policy
    createPolicyIfNotExists(
        jooq,
        schemaName,
        tableName,
        "select_policy_" + safeSchema + "_" + safeTable,
        "SELECT",
        "EXISTS ("
            + "SELECT 1 FROM \"MOLGENIS\".user_permissions_mv u "
            + "WHERE u.user_name = current_user "
            + "AND u.table_schema = "
            + quote(schemaName)
            + " "
            + "AND (u.table_name = "
            + quote(tableName)
            + " OR u.table_name IS NULL) "
            + "AND u.has_select "
            + "AND ((NOT u.is_row_level) OR (u.is_row_level AND u.group_name = ANY(mg_group)))"
            + ")");

    // Create INSERT policy
    createPolicyIfNotExists(
        jooq,
        schemaName,
        tableName,
        "insert_policy_" + safeSchema + "_" + safeTable,
        "INSERT",
        null); // INSERT uses WITH CHECK, handled separately
    createInsertPolicy(jooq, schemaName, tableName, safeSchema, safeTable);

    // Create UPDATE policy
    createPolicyIfNotExists(
        jooq,
        schemaName,
        tableName,
        "update_policy_" + safeSchema + "_" + safeTable,
        "UPDATE",
        "EXISTS ("
            + "SELECT 1 FROM \"MOLGENIS\".user_permissions_mv u "
            + "WHERE u.user_name = current_user "
            + "AND u.table_schema = "
            + quote(schemaName)
            + " "
            + "AND (u.table_name = "
            + quote(tableName)
            + " OR u.table_name IS NULL) "
            + "AND u.has_update "
            + "AND u.group_name = ANY(mg_group)"
            + ")");

    // Create DELETE policy
    createPolicyIfNotExists(
        jooq,
        schemaName,
        tableName,
        "delete_policy_" + safeSchema + "_" + safeTable,
        "DELETE",
        "EXISTS ("
            + "SELECT 1 FROM \"MOLGENIS\".user_permissions_mv u "
            + "WHERE u.user_name = current_user "
            + "AND u.table_schema = "
            + quote(schemaName)
            + " "
            + "AND (u.table_name = "
            + quote(tableName)
            + " OR u.table_name IS NULL) "
            + "AND u.has_delete "
            + "AND u.group_name = ANY(mg_group)"
            + ")");

    logger.info("Enabled RLS on {}.{}", schemaName, tableName);
  }

  /** Disables Row-Level Security on a table. Drops all policies and disables RLS. */
  static void disableRowLevelSecurity(DSLContext jooq, String schemaName, String tableName) {
    String safeSchema = schemaName.replace(" ", "_");
    String safeTable = tableName.replace(" ", "_");

    String[] policyPrefixes = {
      "select_policy_", "insert_policy_", "update_policy_", "delete_policy_"
    };
    for (String prefix : policyPrefixes) {
      String policyName = prefix + safeSchema + "_" + safeTable;
      jooq.execute(
          "DROP POLICY IF EXISTS {0} ON {1}.{2}",
          name(policyName), name(schemaName), name(tableName));
    }

    jooq.execute(
        "ALTER TABLE {0}.{1} DISABLE ROW LEVEL SECURITY", name(schemaName), name(tableName));

    logger.info("Disabled RLS on {}.{}", schemaName, tableName);
  }

  // ── Materialized view refresh ──────────────────────────────────────────────

  static void refreshPermissionsMaterializedView(DSLContext jooq) {
    jooq.execute("REFRESH MATERIALIZED VIEW \"MOLGENIS\".user_permissions_mv");
  }

  // ── Private helpers ────────────────────────────────────────────────────────

  private static void ensureGlobalAdmin(DSLContext jooq, String schemaName) {
    String adminRole = MG_ROLE_PREFIX + "ADMIN";
    boolean adminExists =
        jooq.fetchExists(selectOne().from("pg_roles").where(field("rolname").eq(adminRole)));

    if (!adminExists) {
      saveGroupMetadata(jooq, "ADMIN", List.of());
      ensureRoleExists(jooq, adminRole);
      savePermissions(
          jooq, "ADMIN", new Permission(null, schemaName, false, true, true, true, true, true));
    } else {
      // Ensure admin has permissions on this schema too
      savePermissions(
          jooq, "ADMIN", new Permission(null, schemaName, false, true, true, true, true, true));
    }

    jooq.execute("GRANT ALL ON SCHEMA {0} TO {1}", name(schemaName), name(adminRole));
    jooq.execute("GRANT {0} TO session_user WITH ADMIN OPTION", name(adminRole));
  }

  private static void createGroupWithPermissions(
      DSLContext jooq,
      String schemaName,
      String roleSuffix,
      boolean hasSelect,
      boolean hasInsert,
      boolean hasUpdate,
      boolean hasDelete,
      boolean hasAdmin) {
    String groupName = schemaName + "/" + roleSuffix;
    String pgRole = MG_ROLE_PREFIX + groupName;

    boolean roleExists =
        jooq.fetchExists(selectOne().from("pg_roles").where(field("rolname").eq(pgRole)));

    if (!roleExists) {
      // Save metadata
      saveGroupMetadata(jooq, groupName, List.of());

      // Create the PostgreSQL role
      ensureRoleExists(jooq, pgRole);

      // Save and apply permissions
      Permission perm =
          new Permission(
              null, schemaName, false, hasSelect, hasInsert, hasUpdate, hasDelete, hasAdmin);
      savePermissions(jooq, groupName, perm);

      // Grant USAGE on schema
      jooq.execute("GRANT USAGE ON SCHEMA {0} TO {1}", name(schemaName), name(pgRole));

      // Grant table permissions for existing tables
      List<String> tableNames = getTableNamesInSchema(jooq, schemaName);
      for (String tableName : tableNames) {
        grantTablePermissions(jooq, perm, pgRole, schemaName, tableName);
      }
    }
  }

  private static void grantRoleToRole(
      DSLContext jooq, String schema1, String role1, String schema2, String role2) {
    jooq.execute(
        "GRANT {0} TO {1}", name(roleName(schema1, role1)), name(roleName(schema2, role2)));
  }

  private static String roleName(String schemaName, String roleSuffix) {
    return MG_ROLE_PREFIX + schemaName + "/" + roleSuffix;
  }

  private static void ensureRoleExists(DSLContext jooq, String role) {
    jooq.execute(
        "DO $$\n"
            + "BEGIN\n"
            + "    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = {0}) THEN\n"
            + "        CREATE ROLE {1} WITH NOLOGIN;\n"
            + "    END IF;\n"
            + "END\n"
            + "$$;\n",
        inline(role), name(role));
  }

  private static void grantTablePermissions(
      DSLContext jooq, Permission perm, String pgRole, String schemaName, String tableName) {
    if (perm.hasSelect()) {
      jooq.execute(
          "GRANT SELECT ON TABLE {0}.{1} TO {2}", name(schemaName), name(tableName), name(pgRole));
    }
    if (perm.hasInsert()) {
      jooq.execute(
          "GRANT INSERT ON TABLE {0}.{1} TO {2}", name(schemaName), name(tableName), name(pgRole));
    }
    if (perm.hasUpdate()) {
      jooq.execute(
          "GRANT UPDATE ON TABLE {0}.{1} TO {2}", name(schemaName), name(tableName), name(pgRole));
    }
    if (perm.hasDelete()) {
      jooq.execute(
          "GRANT DELETE ON TABLE {0}.{1} TO {2}", name(schemaName), name(tableName), name(pgRole));
    }
  }

  private static void revokeTablePermissions(
      DSLContext jooq, Permission perm, String pgRole, String schemaName, String tableName) {
    if (perm.hasAdmin() || perm.hasSelect()) {
      jooq.execute(
          "REVOKE SELECT ON TABLE {0}.{1} FROM {2}",
          name(schemaName), name(tableName), name(pgRole));
    }
    if (perm.hasAdmin() || perm.hasInsert()) {
      jooq.execute(
          "REVOKE INSERT ON TABLE {0}.{1} FROM {2}",
          name(schemaName), name(tableName), name(pgRole));
    }
    if (perm.hasAdmin() || perm.hasUpdate()) {
      jooq.execute(
          "REVOKE UPDATE ON TABLE {0}.{1} FROM {2}",
          name(schemaName), name(tableName), name(pgRole));
    }
    if (perm.hasAdmin() || perm.hasDelete()) {
      jooq.execute(
          "REVOKE DELETE ON TABLE {0}.{1} FROM {2}",
          name(schemaName), name(tableName), name(pgRole));
    }
  }

  private static List<String> getTableNamesInSchema(DSLContext jooq, String schemaName) {
    return jooq.select(field(name("table_name"), String.class))
        .from(table(name("MOLGENIS", "table_metadata")))
        .where(field(name("table_schema")).eq(schemaName))
        .fetchInto(String.class);
  }

  private static void createPolicyIfNotExists(
      DSLContext jooq,
      String schemaName,
      String tableName,
      String policyName,
      String operation,
      String usingClause) {
    // Skip INSERT — handled separately because it uses WITH CHECK instead of USING
    if ("INSERT".equals(operation)) return;

    boolean exists =
        jooq.fetchExists(
            selectOne()
                .from("pg_policies")
                .where(
                    field("schemaname").eq(schemaName),
                    field("tablename").eq(tableName),
                    field("policyname").eq(policyName)));
    if (!exists) {
      String sql =
          String.format(
              "CREATE POLICY %s ON %s.%s FOR %s USING (%s)",
              quote(policyName), quote(schemaName), quote(tableName), operation, usingClause);
      jooq.execute(sql);
    }
  }

  private static void createInsertPolicy(
      DSLContext jooq, String schemaName, String tableName, String safeSchema, String safeTable) {
    String policyName = "insert_policy_" + safeSchema + "_" + safeTable;

    boolean exists =
        jooq.fetchExists(
            selectOne()
                .from("pg_policies")
                .where(
                    field("schemaname").eq(schemaName),
                    field("tablename").eq(tableName),
                    field("policyname").eq(policyName)));
    if (exists) return;

    String sql =
        String.format(
            "CREATE POLICY %s ON %s.%s FOR INSERT WITH CHECK ("
                + "EXISTS ("
                + "SELECT 1 FROM \"MOLGENIS\".user_permissions_mv u "
                + "LEFT JOIN unnest(%s.%s.mg_group) AS g(val) "
                + "ON (u.is_row_level AND u.group_name = g.val) "
                + "WHERE u.user_name = current_user "
                + "AND u.table_schema = %s "
                + "AND (u.table_name = %s OR u.table_name IS NULL) "
                + "AND u.has_insert "
                + "GROUP BY u.is_row_level "
                + "HAVING (bool_or(NOT u.is_row_level)) "
                + "OR (bool_and(u.is_row_level) AND count(g.val) = cardinality(%s.%s.mg_group))"
                + ")"
                + ")",
            quote(policyName),
            quote(schemaName),
            quote(tableName),
            quote(schemaName),
            quote(tableName),
            quote(schemaName),
            quote(tableName),
            quote(schemaName),
            quote(tableName));
    jooq.execute(sql);
  }

  /** Quotes an identifier for use in raw SQL strings. */
  private static String quote(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }
}
