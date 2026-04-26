package org.molgenis.emx2.sql.rls;

import static org.jooq.impl.DSL.*;
import static org.molgenis.emx2.Constants.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TablePermission.SelectScope;
import org.molgenis.emx2.TablePermission.UpdateScope;

public class SqlPermissionExecutor {

  private static final String MOLGENIS_SCHEMA = "MOLGENIS";
  private static final String POLICY_PREFIX = "MG_P_";
  private static final String ROLE_PREFIX = "MG_ROLE_";

  private static final String CHANGEOWNER_VERB = "CHANGEOWNER";
  private static final String CHANGEGROUP_VERB = "CHANGEGROUP";
  private static final String SELECT_VERB = "SELECT";

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
      DSLContext jooq, String pgRole, String schema, String table, String verb, UpdateScope scope) {
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

  public static void createSelectScopePolicy(
      DSLContext jooq, String pgRole, String schema, String table, SelectScope scope) {
    String rawRole = stripRolePrefix(pgRole);
    String policyName = composeSelectPolicyName(rawRole, scope);
    String usingExpr = selectScopeUsingExpr(scope);
    jooq.execute(
        "CREATE POLICY {0} ON {1} AS PERMISSIVE FOR SELECT TO {2} USING " + usingExpr,
        name(policyName),
        table(name(schema, table)),
        name(pgRole));
  }

  private static String selectScopeUsingExpr(SelectScope scope) {
    return switch (scope) {
      case NONE -> "(false)";
      case EXISTS, COUNT, AGGREGATE, RANGE -> USING_ALL;
      case OWN -> USING_OWN;
      case GROUP -> USING_GROUP;
      case ALL -> USING_ALL;
    };
  }

  public static void createChangeOwnerPolicy(
      DSLContext jooq, String pgRole, String schema, String table, UpdateScope scope) {
    String rawRole = stripRolePrefix(pgRole);
    String policyName = composeChangeOwnerPolicyName(rawRole, scope);
    jooq.execute(
        "CREATE POLICY {0} ON {1} AS PERMISSIVE FOR SELECT TO {2} USING (false)",
        name(policyName), table(name(schema, table)), name(pgRole));
  }

  public static void createChangeGroupPolicy(
      DSLContext jooq, String pgRole, String schema, String table, UpdateScope scope) {
    String rawRole = stripRolePrefix(pgRole);
    String policyName = composeChangeGroupPolicyName(rawRole, scope);
    jooq.execute(
        "CREATE POLICY {0} ON {1} AS PERMISSIVE FOR SELECT TO {2} USING (false)",
        name(policyName), table(name(schema, table)), name(pgRole));
  }

  public static String composeSelectPolicyName(String rawRole, SelectScope scope) {
    return POLICY_PREFIX + rawRole + "_" + SELECT_VERB + "_" + scope.name();
  }

  public static String composeChangeOwnerPolicyName(String rawRole, UpdateScope scope) {
    return POLICY_PREFIX + rawRole + "_" + CHANGEOWNER_VERB + "_" + scope.name();
  }

  public static String composeChangeGroupPolicyName(String rawRole, UpdateScope scope) {
    return POLICY_PREFIX + rawRole + "_" + CHANGEGROUP_VERB + "_" + scope.name();
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

      String tableKey = schema + ":" + table;
      TablePermission existing = perTable.getOrDefault(tableKey, emptyPermission(schema, table));

      String suffix = policyName.substring((POLICY_PREFIX + rawRole + "_").length());
      existing = applyPolicySuffix(existing, suffix);
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
      TablePermission existing = perTable.getOrDefault(tableKey, emptyPermission(schema, table));

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

  private static TablePermission emptyPermission(String schema, String table) {
    return new TablePermission(schema, table);
  }

  private static TablePermission applyPolicySuffix(TablePermission p, String suffix) {
    if (suffix.startsWith(CHANGEOWNER_VERB + "_")) {
      return new TablePermission(p).setChangeOwner(true);
    }
    if (suffix.startsWith(CHANGEGROUP_VERB + "_")) {
      return new TablePermission(p).setChangeGroup(true);
    }
    if (suffix.startsWith(SELECT_VERB + "_")) {
      String selectName = suffix.substring((SELECT_VERB + "_").length());
      SelectScope parsedSelect = parseSelectScope(selectName);
      if (parsedSelect != null && parsedSelect != SelectScope.NONE) {
        Set<SelectScope> merged = EnumSet.noneOf(SelectScope.class);
        merged.addAll(p.select());
        merged.add(parsedSelect);
        return new TablePermission(p).select(merged);
      }
      return p;
    }
    String[] parts = suffix.split("_", 2);
    if (parts.length < 2) return p;
    String verb = parts[0];
    String scopeName = parts[1];
    UpdateScope scope = parseUpdateScope(scopeName);
    if (scope == null) return p;
    return mergeVerbFromPolicy(p, verb, scope);
  }

  private static SelectScope parseSelectScope(String name) {
    if (name == null) return null;
    try {
      return SelectScope.valueOf(name.toUpperCase(java.util.Locale.ROOT));
    } catch (IllegalArgumentException e) {
      return null;
    }
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

  private static String usingExpr(String verb, UpdateScope scope) {
    if (SQL_INSERT.equals(verb)) return null;
    return switch (scope) {
      case OWN -> USING_OWN;
      case GROUP -> USING_GROUP;
      case ALL -> USING_ALL;
      default -> null;
    };
  }

  private static String withCheckExpr(String verb, UpdateScope scope) {
    if (SQL_SELECT.equals(verb) || SQL_DELETE.equals(verb)) return null;
    if (SQL_UPDATE.equals(verb)) {
      if (scope == UpdateScope.OWN || scope == UpdateScope.GROUP) {
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
      TablePermission p, String verb, UpdateScope scope) {
    return switch (verb.toUpperCase()) {
      case SQL_INSERT -> new TablePermission(p).insert(scope);
      case SQL_UPDATE -> new TablePermission(p).update(scope);
      case SQL_DELETE -> new TablePermission(p).delete(scope);
      default -> p;
    };
  }

  private static UpdateScope parseUpdateScope(String scopeName) {
    return switch (scopeName) {
      case "OWN" -> UpdateScope.OWN;
      case "GROUP" -> UpdateScope.GROUP;
      case "ALL" -> UpdateScope.ALL;
      default -> null;
    };
  }

  private static boolean isVerbScopeNone(TablePermission p, String verb) {
    return switch (verb.toUpperCase()) {
      case SQL_SELECT -> p.select().isEmpty();
      case SQL_INSERT -> p.insert() == UpdateScope.NONE;
      case SQL_UPDATE -> p.update() == UpdateScope.NONE;
      case SQL_DELETE -> p.delete() == UpdateScope.NONE;
      default -> true;
    };
  }

  private static TablePermission mergeVerbAll(TablePermission p, String verb) {
    return switch (verb.toUpperCase()) {
      case SQL_SELECT -> {
        Set<SelectScope> merged = EnumSet.noneOf(SelectScope.class);
        merged.addAll(p.select());
        merged.add(SelectScope.ALL);
        yield new TablePermission(p).select(merged);
      }
      case SQL_INSERT -> new TablePermission(p).insert(UpdateScope.ALL);
      case SQL_UPDATE -> new TablePermission(p).update(UpdateScope.ALL);
      case SQL_DELETE -> new TablePermission(p).delete(UpdateScope.ALL);
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
