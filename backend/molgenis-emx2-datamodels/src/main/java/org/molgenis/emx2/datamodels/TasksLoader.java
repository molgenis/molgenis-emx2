package org.molgenis.emx2.datamodels;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.BOOL;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.LONG;
import static org.molgenis.emx2.ColumnType.TEXT;
import static org.molgenis.emx2.TableMetadata.table;

import org.molgenis.emx2.Schema;

public class TasksLoader implements AvailableDataModels.DataModelLoader {

  private static final String TASKS = "taskInfo";

  // columns
  private static final String ID = "id";
  private static final String DESCRIPTION = "description";
  private static final String STATUS = "status";
  private static final String TOTAL = "total";
  private static final String PROGRESS = "progress";
  private static final String START_TIME_MILLI = "startTimeMilli";
  private static final String END_TIME_MILLI = "endTimeMilli";
  private static final String LOG = "log";
  private static final String STRICT = "strict";

  @Override
  public void load(Schema schema, boolean includeDemoData) {
    schema.create(
        table(TASKS)
            .setDescription("Task executions")
            .add(column(ID).setPkey())
            .add(column(DESCRIPTION))
            .add(column(STATUS))
            .add(column(TOTAL).setType(INT))
            .add(column(PROGRESS).setType(INT))
            .add(column(START_TIME_MILLI).setType(LONG))
            .add(column(END_TIME_MILLI).setType(LONG))
            .add(column(LOG).setType(TEXT))
            .add(column(STRICT).setType(BOOL)));
  }
}
