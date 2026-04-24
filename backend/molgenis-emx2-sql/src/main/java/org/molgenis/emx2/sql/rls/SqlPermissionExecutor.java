package org.molgenis.emx2.sql.rls;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;

import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TablePermission.Scope;

public class SqlPermissionExecutor {

  private static final String MOLGENIS_SCHEMA = "MOLGENIS";
  private static final String POLICY_PREFIX = "MG_P_";
  private static final String ROLE_PREFIX = "MG_ROLE_";

  private static final String USING_OWN = "(mg_owner = current_user)";
  private static final String USING_GROUP =
      "(mg_roles && \"" + MOLGENIS_SCHEMA + "\".current_user_roles())";
  private static final String USING_ALL = "(true)";

  private static final String WITH_CHECK_INSERT_OWN = "(mg_owner = current_user)";
  private static final String WITH_CHECK_INSERT_GROUP =
      "(mg_roles <@ \""
          + MOLGENIS_SCHEMA
          + "\".current_user_roles() AND cardinality(mg_roles) >= 1)";
  private static final String WITH_CHECK_ALL = "(true)";

  private SqlPermissionExecutor() {}

  public static void grantTablePrivilege(
      DSLContext jooq, String role, String schema, String table, String verb) {
    jooq.execute("GRANT " + verb + " ON {0} TO {1}", table(name(schema, table)), name(role));
  }

  public static void revokeTablePrivilege(
      DSLContext jooq, String role, String schema, String table, String verb) {
    jooq.execute("REVOKE " + verb + " ON {0} FROM {1}", table(name(schema, table)), name(role));
  }

  public static void revokeAllTablePrivileges(
      DSLContext jooq, String role, String schema, String table) {
    jooq.execute("REVOKE ALL PRIVILEGES ON {0} FROM {1}", table(name(schema, table)), name(role));
    String policyPattern = POLICY_PREFIX + stripRolePrefix(role) + "_%";
    List<Record> policies =
        jooq.fetch(
            "SELECT policyname FROM pg_policies WHERE schemaname = {0} AND tablename = {1} AND policyname LIKE {2}",
            inline(schema), inline(table), inline(policyPattern));
    for (Record policy : policies) {
      String policyName = policy.get("policyname", String.class);
      jooq.execute(
          "DROP POLICY IF EXISTS {0} ON {1}", name(policyName), table(name(schema, table)));
    }
  }

  public static void createPolicy(
      DSLContext jooq, String pgRole, String schema, String table, String verb, Scope scope) {
    String rawRole = stripRolePrefix(pgRole);
    String policyName = POLICY_PREFIX + rawRole + "_" + verb + "_" + scope.name();
    String usingExpr = usingExpr(verb, scope);
    String withCheckExpr = withCheckExpr(verb, scope);

    if (usingExpr != null && withCheckExpr != null) {
      jooq.execute(
          "CREATE POLICY {0} ON {1} FOR "
              + verb
              + " TO {2} USING "
              + usingExpr
              + " WITH CHECK "
              + withCheckExpr,
          name(policyName),
          table(name(schema, table)),
          name(pgRole));
    } else if (usingExpr != null) {
      jooq.execute(
          "CREATE POLICY {0} ON {1} FOR " + verb + " TO {2} USING " + usingExpr,
          name(policyName),
          table(name(schema, table)),
          name(pgRole));
    } else if (withCheckExpr != null) {
      jooq.execute(
          "CREATE POLICY {0} ON {1} FOR " + verb + " TO {2} WITH CHECK " + withCheckExpr,
          name(policyName),
          table(name(schema, table)),
          name(pgRole));
    }
  }

  public static void dropAllPolicies(DSLContext jooq, String pgRole, String schema, String table) {
    String policyPattern = POLICY_PREFIX + stripRolePrefix(pgRole) + "_%";
    List<Record> policies =
        jooq.fetch(
            "SELECT policyname FROM pg_policies WHERE schemaname = {0} AND tablename = {1} AND policyname LIKE {2}",
            inline(schema), inline(table), inline(policyPattern));
    for (Record policy : policies) {
      String policyName = policy.get("policyname", String.class);
      jooq.execute(
          "DROP POLICY IF EXISTS {0} ON {1}", name(policyName), table(name(schema, table)));
    }
  }

  public static PermissionSet readPolicies(DSLContext jooq, String pgRole) {
    String rawRole = stripRolePrefix(pgRole);
    String policyPattern = POLICY_PREFIX + rawRole + "_%";

    PermissionSet result = new PermissionSet();

    List<Record> policyRows =
        jooq.fetch(
            "SELECT schemaname, tablename, policyname FROM pg_policies WHERE policyname LIKE {0}",
            inline(policyPattern));

    java.util.Map<String, TablePermission> perTable = new java.util.LinkedHashMap<>();

    for (Record row : policyRows) {
      String schema = row.get("schemaname", String.class);
      String table = row.get("tablename", String.class);
      String policyName = row.get("policyname", String.class);

      String[] parts = policyName.split("_");
      if (parts.length < 2) continue;
      String verb = parts[parts.length - 2];
      String scopeName = parts[parts.length - 1];

      String tableKey = schema + ":" + table;
      TablePermission existing =
          perTable.getOrDefault(
              tableKey,
              new TablePermission(
                  schema, table, Scope.NONE, Scope.NONE, Scope.NONE, Scope.NONE, false, false));
      existing = mergeVerbFromPolicy(existing, verb, scopeName);
      if (existing != null) {
        perTable.put(tableKey, existing);
      }
    }

    List<Record> grantRows =
        jooq.fetch(
            "SELECT table_schema, table_name, privilege_type FROM information_schema.role_table_grants WHERE grantee = {0}",
            inline(pgRole));

    for (Record row : grantRows) {
      String schema = row.get("table_schema", String.class);
      String table = row.get("table_name", String.class);
      String verb = row.get("privilege_type", String.class);

      String tableKey = schema + ":" + table;
      TablePermission existing =
          perTable.getOrDefault(
              tableKey,
              new TablePermission(
                  schema, table, Scope.NONE, Scope.NONE, Scope.NONE, Scope.NONE, false, false));

      if (isVerbScopeNone(existing, verb)) {
        existing = mergeVerbAll(existing, verb);
        perTable.put(tableKey, existing);
      }
    }

    for (TablePermission p : perTable.values()) {
      result.put(p);
    }

    return result;
  }

  public static boolean isRlsEnabled(DSLContext jooq, String schema, String table) {
    Record row =
        jooq.fetchOne(
            "SELECT relrowsecurity FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = {0} AND c.relname = {1}",
            inline(schema), inline(table));
    return row != null && Boolean.TRUE.equals(row.get("relrowsecurity", Boolean.class));
  }

  public static void ensureRlsInstalled(DSLContext jooq, String schema, String table) {
    if (!isRlsEnabled(jooq, schema, table)) {
      enableRowLevelSecurity(jooq, schema, table);
      installGuardTrigger(jooq, schema, table);
    }
  }

  public static void enableRowLevelSecurity(DSLContext jooq, String schema, String table) {
    String schemaTable = "\"" + schema + "\".\"" + table + "\"";
    jooq.execute(
        "ALTER TABLE "
            + schemaTable
            + " ADD COLUMN IF NOT EXISTS mg_owner text DEFAULT current_user");
    jooq.execute(
        "ALTER TABLE "
            + schemaTable
            + " ADD COLUMN IF NOT EXISTS mg_roles text[] NOT NULL DEFAULT '{}'");
    boolean hasInsertedBy =
        jooq.fetchExists(
            jooq.select()
                .from(name("information_schema", "columns"))
                .where(
                    field("table_schema")
                        .eq(inline(schema))
                        .and(field("table_name").eq(inline(table)))
                        .and(field("column_name").eq(inline("mg_insertedBy")))));
    if (hasInsertedBy) {
      jooq.execute(
          "UPDATE "
              + schemaTable
              + " SET mg_owner = \"mg_insertedBy\" WHERE mg_owner IS NULL AND \"mg_insertedBy\" IS NOT NULL");
    }
    jooq.execute("ALTER TABLE " + schemaTable + " ALTER COLUMN mg_owner SET NOT NULL");
    jooq.execute("ALTER TABLE " + schemaTable + " ENABLE ROW LEVEL SECURITY");
    jooq.execute("ALTER TABLE " + schemaTable + " FORCE ROW LEVEL SECURITY");
    String indexName = "\"" + table + "_mg_roles_gin\"";
    jooq.execute(
        "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + schemaTable + " USING GIN (mg_roles)");
  }

  public static void disableRowLevelSecurity(DSLContext jooq, String schema, String table) {
    String schemaTable = "\"" + schema + "\".\"" + table + "\"";
    jooq.execute("ALTER TABLE " + schemaTable + " NO FORCE ROW LEVEL SECURITY");
    jooq.execute("ALTER TABLE " + schemaTable + " DISABLE ROW LEVEL SECURITY");
    String indexName = "\"" + schema + "\".\"" + table + "_mg_roles_gin\"";
    jooq.execute("DROP INDEX IF EXISTS " + indexName);
  }

  public static void installGuardTrigger(DSLContext jooq, String schema, String table) {
    String schemaTable = "\"" + schema + "\".\"" + table + "\"";
    String guardFn = "\"" + MOLGENIS_SCHEMA + "\".\"mg_enforce_row_authorisation\"";
    jooq.execute("DROP TRIGGER IF EXISTS mg_enforce_row_authorisation ON " + schemaTable);
    jooq.execute(
        "CREATE TRIGGER mg_enforce_row_authorisation BEFORE UPDATE ON "
            + schemaTable
            + " FOR EACH ROW EXECUTE FUNCTION "
            + guardFn
            + "()");
  }

  public static void dropGuardTrigger(DSLContext jooq, String schema, String table) {
    String schemaTable = "\"" + schema + "\".\"" + table + "\"";
    jooq.execute("DROP TRIGGER IF EXISTS mg_enforce_row_authorisation ON " + schemaTable);
  }

  private static String usingExpr(String verb, Scope scope) {
    if (SQL_INSERT.equals(verb)) return null;
    return switch (scope) {
      case OWN -> USING_OWN;
      case GROUP -> USING_GROUP;
      case ALL -> USING_ALL;
      default -> null;
    };
  }

  private static String withCheckExpr(String verb, Scope scope) {
    if (SQL_SELECT.equals(verb) || SQL_DELETE.equals(verb)) return null;
    if (SQL_UPDATE.equals(verb)) {
      if (scope == Scope.OWN || scope == Scope.GROUP) {
        return WITH_CHECK_ALL;
      }
      return null;
    }
    if (SQL_INSERT.equals(verb)) {
      return switch (scope) {
        case OWN -> WITH_CHECK_INSERT_OWN;
        case GROUP -> WITH_CHECK_INSERT_GROUP;
        case ALL -> WITH_CHECK_ALL;
        default -> null;
      };
    }
    return null;
  }

  private static TablePermission mergeVerbFromPolicy(
      TablePermission p, String verb, String scopeName) {
    Scope scope = parseScope(scopeName);
    if (scope == null) return p;
    return switch (verb.toUpperCase()) {
      case SQL_SELECT ->
          new TablePermission(
              p.schema(),
              p.table(),
              scope,
              p.insert(),
              p.update(),
              p.delete(),
              p.changeOwner(),
              p.share());
      case SQL_INSERT ->
          new TablePermission(
              p.schema(),
              p.table(),
              p.select(),
              scope,
              p.update(),
              p.delete(),
              p.changeOwner(),
              p.share());
      case SQL_UPDATE ->
          new TablePermission(
              p.schema(),
              p.table(),
              p.select(),
              p.insert(),
              scope,
              p.delete(),
              p.changeOwner(),
              p.share());
      case SQL_DELETE ->
          new TablePermission(
              p.schema(),
              p.table(),
              p.select(),
              p.insert(),
              p.update(),
              scope,
              p.changeOwner(),
              p.share());
      default -> p;
    };
  }

  private static Scope parseScope(String scopeName) {
    return switch (scopeName) {
      case "OWN" -> Scope.OWN;
      case "GROUP" -> Scope.GROUP;
      case "ALL" -> Scope.ALL;
      default -> null;
    };
  }

  private static boolean isVerbScopeNone(TablePermission p, String verb) {
    return switch (verb.toUpperCase()) {
      case SQL_SELECT -> p.select() == Scope.NONE;
      case SQL_INSERT -> p.insert() == Scope.NONE;
      case SQL_UPDATE -> p.update() == Scope.NONE;
      case SQL_DELETE -> p.delete() == Scope.NONE;
      default -> true;
    };
  }

  private static TablePermission mergeVerbAll(TablePermission p, String verb) {
    return switch (verb.toUpperCase()) {
      case SQL_SELECT ->
          new TablePermission(
              p.schema(),
              p.table(),
              Scope.ALL,
              p.insert(),
              p.update(),
              p.delete(),
              p.changeOwner(),
              p.share());
      case SQL_INSERT ->
          new TablePermission(
              p.schema(),
              p.table(),
              p.select(),
              Scope.ALL,
              p.update(),
              p.delete(),
              p.changeOwner(),
              p.share());
      case SQL_UPDATE ->
          new TablePermission(
              p.schema(),
              p.table(),
              p.select(),
              p.insert(),
              Scope.ALL,
              p.delete(),
              p.changeOwner(),
              p.share());
      case SQL_DELETE ->
          new TablePermission(
              p.schema(),
              p.table(),
              p.select(),
              p.insert(),
              p.update(),
              Scope.ALL,
              p.changeOwner(),
              p.share());
      default -> p;
    };
  }

  private static String stripRolePrefix(String pgRole) {
    if (pgRole.startsWith(ROLE_PREFIX)) {
      return pgRole.substring(ROLE_PREFIX.length());
    }
    return pgRole;
  }
}
