package org.molgenis.emx2.io;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.RowProcessor;
import org.molgenis.emx2.io.tablestore.TableAndFileStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;

public class ImportTableTask extends Task {
  private Table table;
  private TableStore source;

  public ImportTableTask(TableStore source, Table table, boolean strict) {
    super("Import table " + table.getName(), strict);
    Objects.requireNonNull(source, "tableStore cannot be null");
    Objects.requireNonNull(table, "table cannot be null");
    this.table = table;
    this.source = source;
  }

  @Override
  public void run() {
    this.start();

    // validate uniqueness of the keys in the set
    this.setDescription(
        "Table " + table.getName() + ": Counting rows & checking that all key columns are unique");
    source.processTable(table.getName(), new ValidatePkeyProcessor(table.getMetadata(), this));

    // execute the actual loading, we can use index to find the size
    this.setTotal(this.getProgress());
    this.setDescription("Importing rows into " + table.getName());
    source.processTable(table.getName(), new ImportRowProcesssor(table, this));

    // done
    if (getProgress() > 0) {
      this.complete(String.format("Imported %s %s", getProgress(), table.getName()));
    } else {
      this.setSkipped(String.format("Skipped table %s : sheet was empty", table.getName()));
    }
  }

  public static class ValidatePkeyProcessor implements RowProcessor {

    Set<String> duplicates = new HashSet<>();
    Set<String> keys = new HashSet<>();
    TableMetadata metadata;
    Task task;
    Set<String> warningColumns = new HashSet<>();
    boolean hasEmptyKeys = false;
    List<Column> primaryKeyColumns = new ArrayList<>();

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

      while (iterator.hasNext()) {
        Row row = iterator.next();

        // column warning
        if (task.getProgress() == 0) {
          List<String> columnNames =
              metadata.getDownloadColumnNames().stream().map(Column::getName).toList();
          warningColumns =
              row.getColumnNames().stream()
                  .filter(name -> !columnNames.contains(name))
                  .collect(Collectors.toSet());
          if (!warningColumns.isEmpty()) {
            if (task.isStrict()) {
              throw new MolgenisException(
                  "Found unknown columns "
                      + warningColumns
                      + " in sheet "
                      + metadata.getTableName());
            } else {
              task.addSubTask(
                  "Found unknown columns "
                      + warningColumns
                      + " in sheet "
                      + metadata.getTableName(),
                  TaskStatus.WARNING);
            }
          }
        }

        // primary key(s)
        StringJoiner compoundKey = new StringJoiner(",");
        for (Column column : primaryKeyColumns) {
          if (!row.containsName(column.getName())) {
            if (column.getColumnType() != ColumnType.AUTO_ID) {
              task.addSubTask(
                      "No value provided for key " + column.getName() + " at line " + (index + 1))
                  .setError();
              hasEmptyKeys = true;
            }
          } else {
            String keyValue = row.getString(column.getName());
            compoundKey.add(keyValue);
          }
        }

        String keyValue = compoundKey.toString();
        if (keys.contains(keyValue)) {
          duplicates.add(keyValue);
          String keyFields =
              metadata.getPrimaryKeyFields().stream()
                  .map(Field::getName)
                  .collect(Collectors.joining(","));
          task.addSubTask("Found duplicate Key (" + keyFields + ")=(" + keyValue + ")").setError();
        } else if (!keyValue.isEmpty()) {
          keys.add(keyValue);
        }
        task.setProgress(++index);
      }
      if (!duplicates.isEmpty()) {
        task.completeWithError(
            "Duplicate keys found in table " + metadata.getTableName() + ": " + duplicates);
      }
      if (hasEmptyKeys)
        task.completeWithError("Missing keys found in table " + metadata.getTableName());
    }
  }

  /** executes the import */
  private static class ImportRowProcesssor implements RowProcessor {
    private final Table table;
    private final ImportTableTask task;

    public ImportRowProcesssor(Table table, ImportTableTask task) {
      this.table = table;
      this.task = task;
    }

    @Override
    public void process(Iterator<Row> iterator, TableStore source) {
      task.setProgress(0); // for the progress monitoring
      int index = 0;
      List<Row> batch = new ArrayList<>();
      List<Column> columns = table.getMetadata().getColumns();
      while (iterator.hasNext()) {
        Row row = iterator.next();
        // add file attachments, if applicable
        for (Column c : columns) {
          if (c.isFile()
              && source instanceof TableAndFileStore
              && row.getValueMap().get(c.getName()) != null) {
            row.setBinary(
                c.getName(),
                ((TableAndFileStore) source).getBinaryFileWrapper(row.getString(c.getName())));
          }
        }
        batch.add(row);
        index++;
        if (batch.size() >= 100) {
          table.save(batch);
          task.setProgress(index);
          task.setDescription("Imported " + task.getProgress() + " rows into " + table.getName());
          batch.clear();
        }
      }
      // remaining
      if (!batch.isEmpty()) {
        table.save(batch);
        task.setProgress(index);
        task.setDescription("Imported " + task.getProgress() + " rows into " + table.getName());
      }
    }
  }
}
