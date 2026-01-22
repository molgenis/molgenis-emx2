package org.molgenis.emx2.io.tablestore.processor;

import org.molgenis.emx2.*;
import org.molgenis.emx2.io.ImportTableTask;
import org.molgenis.emx2.io.tablestore.TableAndFileStore;
import org.molgenis.emx2.io.tablestore.TableStore;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.molgenis.emx2.Constants.MG_DELETE;

/** executes the import */
public class ImportRowProcessor implements RowProcessor {

  private final Table table;
  private final ImportTableTask task;

  public ImportRowProcessor(Table table, ImportTableTask task) {
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

      boolean isDrop = row.getValueMap().get(MG_DELETE) != null && row.getBoolean(MG_DELETE);

      // add file attachments, if applicable
      for (Column c : columns) {
        if (c.isFile()
            && source instanceof TableAndFileStore
            && row.getValueMap().get(c.getName()) != null) {
          try {
            BinaryFileWrapper wrapper =
                ((TableAndFileStore) source).getBinaryFileWrapper(row.getString(c.getName()));
            if (row.containsName(c.getName() + "_filename")) {
              wrapper.setFileName(row.getString(c.getName() + "_filename"));
            }
            row.setBinary(c.getName(), wrapper);
          } catch (Exception e) {
            throw new MolgenisException(
                "Failed to read file attachment for table '%s' column '%s' row '%d'"
                    .formatted(table.getName(), c.getName(), index),
                e);
          }
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
}
