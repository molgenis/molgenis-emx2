package org.molgenis.emx2.io.emx2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnAttributeChange;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnRef;
import org.molgenis.emx2.io.emx2.MigrationPlan.ColumnRename;
import org.molgenis.emx2.io.emx2.MigrationPlan.TableRef;
import org.molgenis.emx2.io.emx2.MigrationPlan.TableRename;

/**
 * Desired-state diff between a bundle (desired) and a live schema. DB-free: reads only metadata.
 * Rename inference walks the document-level previousNames chains so a live name any generation back
 * yields a RENAME instead of a DROP+ADD.
 */
public final class ModelDiff {

  private record Attribute(String name, Function<Column, String> accessor) {}

  private static final List<Attribute> ATTRIBUTES =
      List.of(
          new Attribute("type", column -> column.getColumnType().toString()),
          new Attribute("key", column -> String.valueOf(column.getKey())),
          new Attribute("required", Column::getRequired),
          new Attribute("refTable", Column::getRefTableName),
          new Attribute("refLink", Column::getRefLink),
          new Attribute("refLabel", Column::getRefLabel),
          new Attribute("refBack", Column::getRefBack),
          new Attribute("defaultValue", Column::getDefaultValue),
          new Attribute("readonly", column -> String.valueOf(column.isReadonly())),
          new Attribute("visible", Column::getVisible),
          new Attribute("computed", Column::getComputed),
          new Attribute("validation", Column::getValidation),
          new Attribute("values", column -> stringify(column.getValues())),
          new Attribute("semantics", column -> stringify(column.getSemantics())),
          new Attribute("profiles", column -> stringify(column.getProfiles())));

  private static final class Buckets {
    final List<TableRef> tableAdds = new ArrayList<>();
    final List<TableRef> tableDrops = new ArrayList<>();
    final List<TableRename> tableRenames = new ArrayList<>();
    final List<ColumnRef> columnAdds = new ArrayList<>();
    final List<ColumnRef> columnDrops = new ArrayList<>();
    final List<ColumnRename> columnRenames = new ArrayList<>();
    final List<ColumnAttributeChange> changes = new ArrayList<>();
    final List<String> errors = new ArrayList<>();
    final List<String> warnings = new ArrayList<>();

    MigrationPlan toPlan() {
      return new MigrationPlan(
          List.copyOf(tableAdds),
          List.copyOf(tableDrops),
          List.copyOf(tableRenames),
          List.copyOf(columnAdds),
          List.copyOf(columnDrops),
          List.copyOf(columnRenames),
          List.copyOf(changes),
          List.copyOf(errors),
          List.copyOf(warnings));
    }
  }

  private ModelDiff() {}

  public static MigrationPlan diff(Emx2YamlBundle desired, SchemaMetadata live) {
    Buckets buckets = new Buckets();
    SchemaMetadata desiredSchema = desired.schema();
    Set<String> liveTableNames = new LinkedHashSet<>(live.getTableNames());
    Set<String> desiredTableNames = new LinkedHashSet<>(desiredSchema.getTableNames());

    for (String tableName : desiredTableNames) {
      if (liveTableNames.contains(tableName)) {
        diffTable(
            tableName,
            live.getTableMetadata(tableName),
            desiredSchema.getTableMetadata(tableName),
            desired.previousNames().getOrDefault(tableName, Map.of()),
            buckets);
      } else {
        // no table-level previousNames in the format -> absence means add
        buckets.tableAdds.add(new TableRef(tableName));
      }
    }
    // apply is additive: absence never deletes, drops come only from explicit markers
    applyDrops(desired.drops(), live, liveTableNames, buckets);
    return buckets.toPlan();
  }

  private static void applyDrops(
      ModelDrops drops, SchemaMetadata live, Set<String> liveTableNames, Buckets buckets) {
    for (String tableName : drops.tables()) {
      if (liveTableNames.contains(tableName)) {
        buckets.tableDrops.add(new TableRef(tableName));
      }
    }
    for (Map.Entry<String, List<String>> entry : drops.columns().entrySet()) {
      TableMetadata liveTable = live.getTableMetadata(entry.getKey());
      if (liveTable == null) {
        continue;
      }
      Set<String> liveNames = nonSystemColumnNames(liveTable);
      for (String columnName : entry.getValue()) {
        if (liveNames.contains(columnName)) {
          buckets.columnDrops.add(new ColumnRef(entry.getKey(), columnName));
        }
      }
    }
  }

  private static void diffTable(
      String tableName,
      TableMetadata liveTable,
      TableMetadata desiredTable,
      Map<String, List<String>> previousNames,
      Buckets buckets) {
    Set<String> liveNames = nonSystemColumnNames(liveTable);
    Set<String> desiredNames = nonSystemColumnNames(desiredTable);
    Set<String> consumedLive = new LinkedHashSet<>();

    for (String desiredName : desiredNames) {
      Column desiredColumn = desiredTable.getColumn(desiredName);
      if (liveNames.contains(desiredName)) {
        addAttributeChanges(tableName, liveTable.getColumn(desiredName), desiredColumn, buckets);
      } else {
        classifyAbsentColumn(
            tableName,
            desiredColumn,
            previousNames.getOrDefault(desiredName, List.of()),
            liveNames,
            desiredNames,
            liveTable,
            consumedLive,
            buckets);
      }
    }
    // additive: live columns absent from the desired document are left untouched
  }

  private static void classifyAbsentColumn(
      String tableName,
      Column desiredColumn,
      List<String> chain,
      Set<String> liveNames,
      Set<String> desiredNames,
      TableMetadata liveTable,
      Set<String> consumedLive,
      Buckets buckets) {
    String desiredName = desiredColumn.getName();
    String collision = null;
    String liveMatch = null;
    for (String candidate : chain) {
      boolean inLive = liveNames.contains(candidate);
      boolean inDesired = desiredNames.contains(candidate);
      if (inLive && inDesired) {
        collision = candidate;
        break;
      }
      if (inLive && liveMatch == null) {
        liveMatch = candidate;
      }
    }
    if (collision != null) {
      buckets.errors.add(collisionMessage(tableName, desiredName, collision));
    } else if (liveMatch != null) {
      buckets.columnRenames.add(new ColumnRename(tableName, liveMatch, desiredName));
      consumedLive.add(liveMatch);
      addAttributeChanges(tableName, liveTable.getColumn(liveMatch), desiredColumn, buckets);
    } else {
      buckets.columnAdds.add(new ColumnRef(tableName, desiredName));
    }
  }

  private static String collisionMessage(String tableName, String desiredName, String chainName) {
    return "Cannot infer rename for column '"
        + desiredName
        + "': its previous name '"
        + chainName
        + "' is still present in both the live table '"
        + tableName
        + "' and the desired table '"
        + tableName
        + "'. Resolve explicitly (rename or drop the existing column).";
  }

  private static void addAttributeChanges(
      String tableName, Column liveColumn, Column desiredColumn, Buckets buckets) {
    for (Attribute attribute : ATTRIBUTES) {
      String oldValue = attribute.accessor().apply(liveColumn);
      String newValue = attribute.accessor().apply(desiredColumn);
      if (!Objects.equals(oldValue, newValue)) {
        buckets.changes.add(
            new ColumnAttributeChange(
                tableName, desiredColumn.getName(), attribute.name(), oldValue, newValue));
      }
    }
  }

  private static Set<String> nonSystemColumnNames(TableMetadata table) {
    Set<String> result = new LinkedHashSet<>();
    for (Column column : table.getColumns()) {
      if (!column.isSystemColumn()) {
        result.add(column.getName());
      }
    }
    return result;
  }

  private static String stringify(List<String> values) {
    return values == null ? null : values.toString();
  }

  private static String stringify(String[] values) {
    return values == null ? null : Arrays.toString(values);
  }
}
