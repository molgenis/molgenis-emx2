package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.LONG;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.Schema;
import org.molgenis.emx2.tasks.TaskInfo;

public class TasksLoader implements AvailableDataModels.DataModelLoader {

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    schema.create(
        table(TaskInfo.TABLE_NAME)
            .setDescription("Task executions")
            .add(column(TaskInfo.ID).setPkey())
            .add(column(TaskInfo.DESCRIPTION))
            .add(column(TaskInfo.STATUS))
            .add(column(TaskInfo.TOTAL).setType(INT).setRequired(false))
            .add(column(TaskInfo.PROGRESS).setType(INT))
            .add(column(TaskInfo.START_TIME_MILLISECONDS).setType(LONG).setRequired(false))
            .add(column(TaskInfo.END_TIME_MILLISECONDS).setType(LONG).setRequired(false))
            .add(column(TaskInfo.LOG).setType(TEXT).setRequired(false))
            .add(column(TaskInfo.STRICT).setType(BOOL)));
  }
}
