package org.molgenis.emx2.io;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.RowProcessor;
import org.molgenis.emx2.io.tablestore.TableAndFileStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.StepStatus;
import org.molgenis.emx2.tasks.Task;

public class ImportTableTask extends Task {
  private Table table;
  private TableStore source;

  public ImportTableTask(TableStore source, Table table, boolean strict) {
    super("Import table " + table.getName(), strict);
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

    // execute the actual loading
    this.setTotal(this.getIndex());
    this.setDescription("Importing rows into " + table.getName());
    source.processTable(table.getName(), new ImportRowProcesssor(table, this));

    // done
    if (getIndex() > 0) {
      this.complete(String.format("Imported %s %s", getIndex(), table.getName()));
    } else {
      this.skipped(String.format("Skipped table %s : sheet was empty", table.getName()));
    }
  }

  public static class ValidatePkeyProcessor implements RowProcessor {

    Set<String> duplicates = new HashSet<>();
    Set<String> keys = new HashSet<>();
    TableMetadata metadata;
    Task task;
    Set<String> warningColumns = new HashSet<>();

    public ValidatePkeyProcessor(TableMetadata metadata, Task task) {
      this.metadata = metadata;
      this.task = task;
    }

    @Override
    public void process(Iterator<Row> iterator, TableStore source) {

      task.setIndex(0);
      int index = 0;

      while (iterator.hasNext()) {
        Row row = iterator.next();

        // column warning
        if (task.getIndex() == 0) {
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
              task.step(
                  "Found unknown columns "
                      + warningColumns
                      + " in sheet "
                      + metadata.getTableName(),
                  StepStatus.WARNING);
            }
          }
        }

        // primary key
        String keyValue =
            metadata.getPrimaryKeyFields().stream()
                .map(f -> row.getString(f.getName()))
                .collect(Collectors.joining(","));
        if (keys.contains(keyValue)) {
          duplicates.add(keyValue);
          String keyFields =
              metadata.getPrimaryKeyFields().stream()
                  .map(Field::getName)
                  .collect(Collectors.joining(","));
          task.step("Found duplicate Key (" + keyFields + ")=(" + keyValue + ")").error();
        } else {
          keys.add(keyValue);
        }
        task.setIndex(++index);
      }
      if (!duplicates.isEmpty()) {
        task.completeWithError(
            "Duplicate keys found in table " + metadata.getTableName() + ": " + duplicates);
      }
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
      task.setIndex(0);
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
        if (batch.size() >= 1000) {
          table.save(batch);
          task.setIndex(index);
          task.setDescription("Imported " + task.getIndex() + " rows into " + table.getName());
          batch.clear();
        }
      }
      // remaining
      if (!batch.isEmpty()) {
        table.save(batch);
        task.setIndex(index);
        task.setDescription("Imported " + task.getIndex() + " rows into " + table.getName());
      }
    }
  }
}
