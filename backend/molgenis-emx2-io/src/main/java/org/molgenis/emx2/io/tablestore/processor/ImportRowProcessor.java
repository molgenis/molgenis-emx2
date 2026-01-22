package org.molgenis.emx2.io.tablestore.processor;

import static org.molgenis.emx2.Constants.MG_DELETE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.TableAndFileStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.tasks.Task;

/** executes the import */
public class ImportRowProcessor implements RowProcessor {

  private final Table table;
  private final Task task;

  public ImportRowProcessor(Table table, Task task) {
    this.table = table;
    this.task = task;
  }

  @Override
  public void process(Iterator<Row> iterator, TableStore source) {
    task.setProgress(0); // for the progress monitoring
    int index = 0;
    List<Row> importBatch = new ArrayList<>();
    List<Row> deleteBatch = new ArrayList<>();
    List<Column> columns = table.getMetadata().getColumns();
    while (iterator.hasNext()) {
      Row row = iterator.next();

      if (row.getValueMap().isEmpty()) {
        continue;
      }

      boolean isDrop = row.getValueMap().get(MG_DELETE) != null && row.getBoolean(MG_DELETE);

      // add file attachments, if applicable
      for (Column c : columns) {
        if (cellRefersToAttachment(source, c, row)) {
          BinaryFileWrapper fileWrapper = getFileWrapper((TableAndFileStore) source, c, row, index);
          row.setBinary(c.getName(), fileWrapper);
        }
      }

      if (!isDrop) {
        importBatch.add(row);
      } else {
        deleteBatch.add(row);
      }
      index++;

      if (importBatch.size() >= 100) {
        table.save(importBatch);
        task.setProgress(index);
        task.setDescription("Imported " + task.getProgress() + " rows into " + table.getName());
        importBatch.clear();
      }
    }
    // remaining
    if (!importBatch.isEmpty()) {
      table.save(importBatch);
      task.setProgress(index);
      task.setDescription("Imported " + task.getProgress() + " rows into " + table.getName());
    }
    // delete
    if (!deleteBatch.isEmpty()) {
      table.delete(deleteBatch);
      task.setProgress(deleteBatch.size());
      task.setDescription("Deleted " + task.getProgress() + " rows from " + table.getName());
    }
  }

  private BinaryFileWrapper getFileWrapper(
      TableAndFileStore source, Column column, Row row, int index) {
    try {
      BinaryFileWrapper wrapper = source.getBinaryFileWrapper(row.getString(column.getName()));
      if (row.containsName(column.getName() + "_filename")) {
        wrapper.setFileName(row.getString(column.getName() + "_filename"));
      }

      return wrapper;
    } catch (Exception e) {
      throw new MolgenisException(
          "Failed to read file attachment for table '%s' column '%s' row '%d'"
              .formatted(table.getName(), column.getName(), index),
          e);
    }
  }

  private static boolean cellRefersToAttachment(TableStore source, Column column, Row row) {
    return column.isFile()
        && source instanceof TableAndFileStore
        && row.getValueMap().get(column.getName()) != null;
  }
}
