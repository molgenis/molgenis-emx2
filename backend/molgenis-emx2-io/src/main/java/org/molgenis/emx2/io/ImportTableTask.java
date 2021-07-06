package org.molgenis.emx2.io;

import java.util.*;
import java.util.stream.Collectors;
import org.jooq.Field;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.RowProcessor;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

public class ImportTableTask extends Task {
  private Table table;
  private TableStore source;

  public ImportTableTask(TableStore source, Table table) {
    super("Import table " + table.getName());
    this.table = table;
    this.source = source;
  }

  public void run() {
    this.start();

    // validate column names, provide warning if some columns will be ignored

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
      this.complete("Imported " + getIndex() + " " + table.getName());
    } else {
      this.skipped("Skipped table " + table.getName() + ": sheet was empty");
    }
  }

  public static class ValidatePkeyProcessor implements RowProcessor {

    Set<String> duplicates = new HashSet<>();
    Set<String> keys = new HashSet<>();
    TableMetadata metadata;
    Task task;

    public ValidatePkeyProcessor(TableMetadata metadata, Task task) {
      this.metadata = metadata;
      this.task = task;
    }

    @Override
    public void process(Iterator<Row> iterator) {

      task.setIndex(0);

      while (iterator.hasNext()) {
        Row row = iterator.next();
        String keyValue = "";
        for (Field f : metadata.getPrimaryKeyFields()) {
          keyValue += row.getString(f.getName()) + ",";
        }
        keyValue = keyValue.substring(0, keyValue.length() - 1);
        if (keys.contains(keyValue)) {
          duplicates.add(keyValue);
          String keyFields =
              metadata.getPrimaryKeyFields().stream()
                  .map(f -> f.getName())
                  .collect(Collectors.joining(","));
          task.step("Found duplicate Key (" + keyFields + ")=(" + keyValue + ")").error();
        } else {
          keys.add(keyValue);
        }
        task.setIndex(task.getIndex());
      }
      if (duplicates.size() > 0) {
        task.completeWithError(
            "Duplicate keys found in table " + metadata.getTableName() + ": " + duplicates);
      }
    }
  }

  /** executes the import */
  private static class ImportRowProcesssor implements RowProcessor {
    private final Table table;
    private final Task task;

    public ImportRowProcesssor(Table table, Task task) {
      this.table = table;
      this.task = task;
    }

    @Override
    public void process(Iterator<Row> iterator) {

      task.setIndex(0);
      int index = 0;
      List<Row> batch = new ArrayList<>();
      while (iterator.hasNext()) {
        batch.add(iterator.next());
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
