package org.molgenis.emx2.web;

import static org.molgenis.emx2.web.Constants.ACCEPT_JSON;
import static org.molgenis.emx2.web.Constants.ACCEPT_YAML;
import static org.molgenis.emx2.web.MolgenisWebservice.getSchema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionEvaluator;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.TableType;
import org.molgenis.emx2.io.emx2.BundleValidator;
import org.molgenis.emx2.io.emx2.CompanionSchemas;
import org.molgenis.emx2.io.emx2.CompanionSchemas.CompanionDeclaration;
import org.molgenis.emx2.io.emx2.Emx2Yaml;
import org.molgenis.emx2.io.emx2.Emx2YamlBundle;
import org.molgenis.emx2.io.emx2.MigrationPlan;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnAttributeChange;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnRef;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnRename;
import org.molgenis.emx2.io.emx2.MigrationPlan.TableRef;
import org.molgenis.emx2.io.emx2.ModelDiff;

public class ModelApi {

  public static final String MG_MODEL_VERSION = "mg_model_version";
  public static final String MG_PREVIOUS_NAMES = "mg_previous_names";

  private static final String ROOT_FILE = "molgenis.yaml";
  private static final String IGNORED_DATA_WARNING =
      "ignoring 'data' key: bundle data content is loaded by templates/loaders, not the model API";
  private static final String IGNORED_DEMO_WARNING =
      "ignoring 'demo' key: bundle demo content is loaded by templates/loaders, not the model API";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final TypeReference<Map<String, Map<String, List<String>>>> PREVIOUS_NAMES_TYPE =
      new TypeReference<>() {};

  private ModelApi() {
    // hide constructor
  }

  public static void create(Javalin app) {
    final String modelPath = "/{schema}/api/model";
    app.get(modelPath, ModelApi::getModel);
    app.put(modelPath, ModelApi::putModel);
  }

  static void getModel(Context ctx) {
    Schema schema = getSchema(ctx);
    String storedVersion = schema.getMetadata().getSetting(MG_MODEL_VERSION);
    Emx2YamlBundle bundle =
        new Emx2YamlBundle(
            schema.getMetadata(),
            Emx2Yaml.SUPPORTED_FORMAT_VERSION,
            storedVersion,
            Map.of(),
            loadPreviousNames(schema.getMetadata()));
    ctx.contentType(ACCEPT_YAML);
    ctx.status(200);
    ctx.result(Emx2Yaml.toSingleFile(bundle));
  }

  static void putModel(Context ctx) throws Exception {
    Schema schema = getSchema(ctx);
    boolean dryRun = "true".equals(ctx.queryParam("dryRun"));

    // parse + validate; formatVersion skew, unknown keys and companion cycles fail here first
    Emx2YamlBundle parsed = BundleValidator.validate(Map.of(ROOT_FILE, ctx.body()));
    List<CompanionDeclaration> companions = CompanionSchemas.fromSingleFile(ctx.body());
    stripBareOntologyTables(parsed.schema());
    Map<String, Map<String, List<String>>> mergedPreviousNames =
        mergePreviousNames(loadPreviousNames(schema.getMetadata()), parsed.previousNames());
    Emx2YamlBundle bundle =
        new Emx2YamlBundle(
            parsed.schema(),
            parsed.formatVersion(),
            parsed.version(),
            parsed.namespaces(),
            mergedPreviousNames,
            parsed.drops());
    MigrationPlan plan = ModelDiff.diff(bundle, schema.getMetadata());
    String storedVersion = schema.getMetadata().getSetting(MG_MODEL_VERSION);
    List<String> companionWarnings = companionWarnings(schema.getDatabase(), companions);
    companionWarnings.addAll(ignoredKeyWarnings(parsed));

    if (dryRun) {
      ctx.contentType(ACCEPT_JSON);
      ctx.status(200);
      ctx.result(
          JSON.writeValueAsString(
              response(false, bundle.version(), storedVersion, plan, companionWarnings)));
      return;
    }

    if (!PermissionEvaluator.canManage(schema)) {
      throw new MolgenisException("Unauthorized to update schema model");
    }
    if (!plan.errors().isEmpty()) {
      ctx.contentType(ACCEPT_JSON);
      ctx.status(400);
      ctx.result(
          JSON.writeValueAsString(
              response(false, bundle.version(), storedVersion, plan, companionWarnings)));
      return;
    }
    if (isDowngrade(bundle.version(), storedVersion)) {
      throw new MolgenisException(
          "Refusing to apply model version '"
              + bundle.version()
              + "' because it is older than the stored version '"
              + storedVersion
              + "'; bump the version to apply");
    }

    applyInTransaction(schema, bundle, plan, companions);
    ctx.contentType(ACCEPT_JSON);
    ctx.status(200);
    ctx.result(
        JSON.writeValueAsString(
            response(true, bundle.version(), bundle.version(), plan, companionWarnings)));
  }

  private static Map<String, Object> response(
      boolean applied,
      String requestedVersion,
      String storedVersion,
      MigrationPlan plan,
      List<String> companionWarnings) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("applied", applied);
    body.put("version", requestedVersion);
    body.put("storedVersion", storedVersion);
    body.put("plan", plan);
    body.put("companionWarnings", companionWarnings);
    return body;
  }

  private static List<String> ignoredKeyWarnings(Emx2YamlBundle parsed) {
    List<String> warnings = new ArrayList<>();
    if (!parsed.dataFiles().isEmpty()) {
      warnings.add(IGNORED_DATA_WARNING);
    }
    if (!parsed.demoFiles().isEmpty()) {
      warnings.add(IGNORED_DEMO_WARNING);
    }
    return warnings;
  }

  private static List<String> companionWarnings(
      Database database, List<CompanionDeclaration> companions) {
    List<String> warnings = new ArrayList<>();
    for (CompanionDeclaration companion : companions) {
      if (!companion.hasInlineModel()
          || companion.model().version() == null
          || !database.hasSchema(companion.name())) {
        continue;
      }
      String stored =
          database.getSchema(companion.name()).getMetadata().getSetting(MG_MODEL_VERSION);
      String referenced = companion.model().version();
      if (stored != null && compareVersions(stored, referenced) < 0) {
        warnings.add(
            "companion schema '"
                + companion.name()
                + "' has stored version '"
                + stored
                + "' which is older than the referenced version '"
                + referenced
                + "'; an existing companion is never modified");
      }
    }
    return warnings;
  }

  private static void applyInTransaction(
      Schema schema,
      Emx2YamlBundle bundle,
      MigrationPlan plan,
      List<CompanionDeclaration> companions) {
    schema.tx(
        database -> {
          for (CompanionDeclaration companion : companions) {
            provisionCompanion(database, companion);
          }
          Schema txSchema = database.getSchema(schema.getName());
          applyPlan(txSchema, bundle.schema(), plan);
          if (bundle.version() != null) {
            txSchema.getMetadata().setSetting(MG_MODEL_VERSION, bundle.version());
          }
          persistPreviousNames(txSchema, bundle.previousNames());
        });
  }

  private static Map<String, Map<String, List<String>>> loadPreviousNames(SchemaMetadata schema) {
    String stored = schema.getSetting(MG_PREVIOUS_NAMES);
    if (stored == null || stored.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      Map<String, Map<String, List<String>>> parsed = JSON.readValue(stored, PREVIOUS_NAMES_TYPE);
      return parsed == null ? new LinkedHashMap<>() : parsed;
    } catch (JsonProcessingException exception) {
      throw new MolgenisException("Failed to read stored rename chains", exception);
    }
  }

  private static Map<String, Map<String, List<String>>> mergePreviousNames(
      Map<String, Map<String, List<String>>> persisted,
      Map<String, Map<String, List<String>>> incoming) {
    Map<String, Map<String, List<String>>> merged = new LinkedHashMap<>();
    for (Map.Entry<String, Map<String, List<String>>> table : persisted.entrySet()) {
      merged.put(table.getKey(), new LinkedHashMap<>(table.getValue()));
    }
    for (Map.Entry<String, Map<String, List<String>>> table : incoming.entrySet()) {
      merged.computeIfAbsent(table.getKey(), key -> new LinkedHashMap<>()).putAll(table.getValue());
    }
    return merged;
  }

  private static void persistPreviousNames(
      Schema txSchema, Map<String, Map<String, List<String>>> previousNames) {
    if (previousNames.isEmpty()) {
      return;
    }
    try {
      txSchema.getMetadata().setSetting(MG_PREVIOUS_NAMES, JSON.writeValueAsString(previousNames));
    } catch (JsonProcessingException exception) {
      throw new MolgenisException("Failed to persist rename chains", exception);
    }
  }

  private static void provisionCompanion(Database database, CompanionDeclaration companion) {
    if (database.hasSchema(companion.name())) {
      // an existing companion is never modified by another bundle's apply
      return;
    }
    if (!companion.hasInlineModel()) {
      throw new MolgenisException(
          "companion schema '"
              + companion.name()
              + "' is declared by bundle reference '"
              + companion.bundleRef()
              + "'; a single-file model upload must inline the companion content");
    }
    try {
      Schema created = database.createSchema(companion.name());
      SchemaMetadata model = companion.model().schema();
      stripBareOntologyTables(model);
      created.migrate(model);
      applyPermissions(created, companion.permissions());
      if (companion.model().version() != null) {
        created.getMetadata().setSetting(MG_MODEL_VERSION, companion.model().version());
      }
    } catch (MolgenisException cause) {
      throw new MolgenisException(
          "Failed to apply companion schema '" + companion.name() + "': " + cause.getMessage(),
          cause);
    }
  }

  private static void applyPermissions(Schema schema, Map<String, String> permissions) {
    for (Map.Entry<String, String> entry : permissions.entrySet()) {
      String role = entry.getKey();
      if (!Privileges.isSystemRole(role)) {
        throw new MolgenisException(
            "unknown permission role '"
                + role
                + "' in companion schema '"
                + schema.getName()
                + "'; legal roles are "
                + legalRoleNames());
      }
      schema.addMember(entry.getValue(), role);
    }
  }

  private static String legalRoleNames() {
    return Arrays.stream(Privileges.values())
        .map(Privileges::toString)
        .collect(Collectors.joining(", "));
  }

  private static void stripBareOntologyTables(SchemaMetadata schema) {
    List<String> bare = new ArrayList<>();
    for (TableMetadata table : schema.getTables()) {
      if (TableType.ONTOLOGIES.equals(table.getTableType()) && isBare(table)) {
        bare.add(table.getTableName());
      }
    }
    for (String tableName : bare) {
      schema.drop(tableName);
    }
  }

  private static boolean isBare(TableMetadata table) {
    for (Column column : table.getColumns()) {
      if (!column.isSystemColumn()) {
        return false;
      }
    }
    return true;
  }

  private static void applyPlan(Schema txSchema, SchemaMetadata desired, MigrationPlan plan) {
    Set<String> tablesToAdd = new LinkedHashSet<>();
    for (TableRef add : plan.tableAdds()) {
      tablesToAdd.add(add.table());
    }
    for (String tableName : orderTableAddsByInheritance(desired, tablesToAdd)) {
      txSchema.create(desired.getTableMetadata(tableName));
    }
    Set<String> renamedTargets = new LinkedHashSet<>();
    for (ColumnRename rename : plan.columnRenames()) {
      Column desiredColumn = desired.getTableMetadata(rename.table()).getColumn(rename.toColumn());
      txSchema
          .getTable(rename.table())
          .getMetadata()
          .alterColumn(rename.fromColumn(), desiredColumn);
      renamedTargets.add(columnKey(rename.table(), rename.toColumn()));
    }
    for (ColumnRef add : plan.columnAdds()) {
      Column desiredColumn = desired.getTableMetadata(add.table()).getColumn(add.column());
      txSchema.getTable(add.table()).getMetadata().add(desiredColumn);
    }
    applyAttributeChanges(txSchema, desired, plan.changes(), renamedTargets);
    for (ColumnRef drop : plan.columnDrops()) {
      txSchema.getTable(drop.table()).getMetadata().dropColumn(drop.column());
    }
    List<TableRef> drops = plan.tableDrops();
    for (int index = drops.size() - 1; index >= 0; index--) {
      txSchema.dropTable(drops.get(index).table());
    }
  }

  private static List<String> orderTableAddsByInheritance(
      SchemaMetadata desired, Set<String> tablesToAdd) {
    List<String> ordered = new ArrayList<>();
    Set<String> visited = new LinkedHashSet<>();
    for (String tableName : desired.getTableNames()) {
      if (tablesToAdd.contains(tableName)) {
        visitTableAdd(tableName, desired, tablesToAdd, visited, ordered);
      }
    }
    return ordered;
  }

  private static void visitTableAdd(
      String tableName,
      SchemaMetadata desired,
      Set<String> tablesToAdd,
      Set<String> visited,
      List<String> ordered) {
    if (!visited.add(tableName)) {
      return;
    }
    for (String parent : desired.getTableMetadata(tableName).getInheritNames()) {
      if (tablesToAdd.contains(parent)) {
        visitTableAdd(parent, desired, tablesToAdd, visited, ordered);
      }
    }
    ordered.add(tableName);
  }

  private static void applyAttributeChanges(
      Schema txSchema,
      SchemaMetadata desired,
      List<ColumnAttributeChange> changes,
      Set<String> renamedTargets) {
    Set<String> altered = new LinkedHashSet<>();
    for (ColumnAttributeChange change : changes) {
      String key = columnKey(change.table(), change.column());
      if (renamedTargets.contains(key) || !altered.add(key)) {
        continue;
      }
      Column desiredColumn = desired.getTableMetadata(change.table()).getColumn(change.column());
      txSchema.getTable(change.table()).getMetadata().alterColumn(change.column(), desiredColumn);
    }
  }

  private static String columnKey(String table, String column) {
    return table + " " + column;
  }

  static boolean isDowngrade(String requested, String stored) {
    if (requested == null || stored == null) {
      return false;
    }
    return compareVersions(requested, stored) < 0;
  }

  private static int compareVersions(String left, String right) {
    String[] leftParts = left.split("\\.");
    String[] rightParts = right.split("\\.");
    int length = Math.max(leftParts.length, rightParts.length);
    for (int index = 0; index < length; index++) {
      int comparison = compareSegment(segment(leftParts, index), segment(rightParts, index));
      if (comparison != 0) {
        return comparison;
      }
    }
    return 0;
  }

  private static String segment(String[] parts, int index) {
    return index < parts.length ? parts[index] : "0";
  }

  private static int compareSegment(String left, String right) {
    Integer leftNumber = asInteger(left);
    Integer rightNumber = asInteger(right);
    if (leftNumber != null && rightNumber != null) {
      return Integer.compare(leftNumber, rightNumber);
    }
    return left.compareTo(right);
  }

  private static Integer asInteger(String value) {
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException exception) {
      return null;
    }
  }
}
