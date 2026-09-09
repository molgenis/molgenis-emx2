package org.molgenis.emx2.fairmapper.postprocessing;

import java.util.*;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

/**
 * Merges rows that share the same primary key value and don't conflict on any other column. Two
 * rows are duplicates of each other when, for every column, at most one of them carries a value; if
 * both carry a differing value they describe different things and are kept as separate rows. Rows
 * missing part of their primary key can't be identified as the same row and are left untouched.
 * Columns produced by the transform step to track the source IRI ("_subject_",
 * "_subject_&lt;column&gt;") are ignored when comparing, since they don't describe the data itself.
 */
public class DeduplicatingPostProcessor implements PostProcessor {

  private static final String SUBJECT_COLUMN_PREFIX = "_subject_";

  private final SchemaMetadata schema;

  public DeduplicatingPostProcessor(SchemaMetadata schema) {
    this.schema = schema;
  }

  @Override
  public void process(InMemoryTableStore tableStore) {
    for (String tableName : tableStore.getTableNames()) {
      List<Column> pkColumns = schema.getTableMetadata(tableName).getPrimaryKeyColumns();
      List<Row> deduplicated = deduplicate(tableStore.readTable(tableName), pkColumns);

      tableStore.writeTable(
          tableName,
          deduplicated.stream().flatMap(row -> row.getColumnNames().stream()).distinct().toList(),
          deduplicated);
    }
  }

  private static List<Row> deduplicate(Iterable<Row> rows, List<Column> pkColumns) {
    List<Row> result = new ArrayList<>();
    for (Row row : rows) {

      Optional<Row> duplicate = findDuplicate(result, row, pkColumns);
      if (duplicate.isEmpty()) {
        result.add(row);
      } else {
        mergeInto(duplicate.get(), row);
      }
    }
    return result;
  }

  private static boolean hasAllPkValues(Row row, List<Column> pkColumns) {
    return pkColumns.stream().allMatch(pk -> row.notNull(pk.getName()));
  }

  private static Optional<Row> findDuplicate(
      List<Row> candidates, Row row, List<Column> pkColumns) {
    if (!hasAllPkValues(row, pkColumns)) {
      return Optional.empty();
    }

    return candidates.stream()
        .filter(candidate -> matchesPrimaryKey(candidate, row, pkColumns))
        .filter(candidate -> !hasConflictingValues(candidate, row))
        .findFirst();
  }

  private static boolean matchesPrimaryKey(Row row1, Row row2, List<Column> pkColumns) {
    return pkColumns.stream().allMatch(pk -> valuesEqual(rawValue(row1, pk), rawValue(row2, pk)));
  }

  private static boolean hasConflictingValues(Row row1, Row row2) {
    Set<String> columnNames = new LinkedHashSet<>(row1.getColumnNames());
    columnNames.addAll(row2.getColumnNames());

    for (String columnName : columnNames) {
      if (isSubjectColumn(columnName)) {
        continue;
      }
      if (row1.notNull(columnName)
          && row2.notNull(columnName)
          && !valuesEqual(rawValue(row1, columnName), rawValue(row2, columnName))) {
        return true;
      }
    }
    return false;
  }

  private static void mergeInto(Row target, Row source) {
    for (String columnName : source.getColumnNames()) {
      if (!target.notNull(columnName) && source.notNull(columnName)) {
        target.set(columnName, rawValue(source, columnName));
      }
    }
  }

  private static boolean isSubjectColumn(String columnName) {
    return columnName.startsWith(SUBJECT_COLUMN_PREFIX);
  }

  private static Object rawValue(Row row, Column column) {
    return rawValue(row, column.getName());
  }

  private static Object rawValue(Row row, String columnName) {
    return row.getValueMap().get(columnName);
  }

  private static boolean valuesEqual(Object value1, Object value2) {
    if (value1 == null || value2 == null) {
      return value1 == value2;
    }
    if (value1 instanceof Object[] array1 && value2 instanceof Object[] array2) {
      return Arrays.equals(array1, array2);
    }
    return value1.equals(value2);
  }
}
