package org.molgenis.emx2.io.tablestore.processor;

import static org.molgenis.emx2.Constants.MG_DELETE;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;
import org.molgenis.emx2.utils.generator.AutoIdFormat;

public class ValidatePkeyProcessor implements RowProcessor {

  private final TableMetadata metadata;
  private final Task task;

  private final Set<String> duplicates = new HashSet<>();
  private final Set<String> keys = new HashSet<>();
  private final Set<String> invalidValues = new HashSet<>();

  private final List<Column> primaryKeyColumns = new ArrayList<>();
  private Set<String> warningColumns = new HashSet<>();

  private boolean hasEmptyKeys = false;

  public ValidatePkeyProcessor(TableMetadata metadata, Task task) {
    this.metadata = metadata;
    this.task = task;
    this.processColumns();
  }

  private void processColumns() {
    for (Column column : metadata.getPrimaryKeyColumns()) {
      if (column.isReference()) {
        for (Reference ref : column.getReferences()) {
          primaryKeyColumns.add(ref.toPrimitiveColumn());
        }
      } else {
        primaryKeyColumns.add(column);
      }
    }
  }

  @Override
  public void process(Iterator<Row> iterator, TableStore source) {
    task.setProgress(0);
    int index = 0;
    String errorMessage = null;
    while (iterator.hasNext() && errorMessage == null) {
      Row row = iterator.next();

      if (row.isEmpty()) {
        continue;
      }

      // column warning
      if (task.getProgress() == 0) {
        List<String> columnNames =
            new ArrayList<>(
                metadata.getDownloadColumnNames().stream().map(Column::getName).toList());
        columnNames.add(MG_DELETE);

        warningColumns =
            row.getColumnNames().stream()
                .filter(name -> !columnNames.contains(name))
                .collect(Collectors.toSet());

        checkWarningColumns();
      }

      // primary key(s)
      StringJoiner compoundKey = new StringJoiner(",");
      for (Column column : primaryKeyColumns) {
        if (row.containsName(column.getName())) {
          String keyValue = row.getString(column.getName());
          compoundKey.add(keyValue);

          if (column.getColumnType() == ColumnType.AUTO_ID) {
            validateAutoIdValue(keyValue, column);
          }
        } else if (column.getColumnType() != ColumnType.AUTO_ID) {
          task.addSubTask(
                  "No value provided for key " + column.getName() + " at line " + (index + 1))
              .setError();
          hasEmptyKeys = true;
          errorMessage = "missing value for key column '" + column.getName() + "'. Row: " + row;
        }
      }

      String keyValue = compoundKey.toString();
      validateKeyValue(keyValue);

      task.setProgress(++index);
    }

    if (!duplicates.isEmpty()) {
      task.completeWithError(
          "Duplicate keys found in table " + metadata.getTableName() + ": " + duplicates);
    }

    if (!invalidValues.isEmpty()) {
      task.completeWithError(
          "Invalid values found in table " + metadata.getTableName() + ": " + invalidValues);
    }

    if (hasEmptyKeys)
      task.completeWithError(
          "Missing keys found in table '" + metadata.getTableName() + "': " + errorMessage);
  }

  private void validateAutoIdValue(String value, Column column) {
    String computed = column.getComputed();
    if (computed == null) {
      return;
    }

    AutoIdFormat format = AutoIdFormat.fromComputedString(computed);
    if (!format.valueCompliesToFormat(value)) {
      task.addSubTask("Found invalid auto-id value: " + value + ", for column: " + column.getName())
          .setError();
      invalidValues.add(value);
    }
  }

  private void checkWarningColumns() {
    if (!warningColumns.isEmpty()) {
      if (task.isStrict()) {
        throw new MolgenisException(
            "Found unknown columns " + warningColumns + " in sheet " + metadata.getTableName());
      } else {
        task.addSubTask(
            "Found unknown columns " + warningColumns + " in sheet " + metadata.getTableName(),
            TaskStatus.WARNING);
      }
    }
  }

  private void validateKeyValue(String keyValue) {
    if (keys.contains(keyValue)) {
      duplicates.add(keyValue);
      String keyFields =
          metadata.getPrimaryKeyFields().stream()
              .map(Field::getName)
              .collect(Collectors.joining(","));

      task.addSubTask("Found duplicate Key (" + keyFields + ")=(" + keyValue + ")").setError();
    } else if (!keyValue.isEmpty() && !keyValue.equals("null")) {
      keys.add(keyValue);
    }
  }
}
