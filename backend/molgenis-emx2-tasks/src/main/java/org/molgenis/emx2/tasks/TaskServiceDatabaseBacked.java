package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.FilterBean.or;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;

public class TaskServiceDatabaseBacked extends TaskServiceInMemory {
  private Schema systemSchema;

  public TaskServiceDatabaseBacked(Schema systemSchema) {
    this.systemSchema = systemSchema;
    this.init();
  }

  @Override
  public String submit(Task task) {
    // we insert task to database
    save(task);

    // and we add a change handler to write task status changes to database
    task.setChangedHandler(changedTask -> save(changedTask));
    return super.submit(task);
  }

  private void save(Task task) {
    Row jobRow =
        row(
            "id",
            task.getId(),
            "status",
            task.getStatus(),
            "description",
            task.getDescription(),
            "submission",
            new Date(task.getSubmitTimeMilliseconds()),
            "start",
            new Date(task.getStartTimeMilliseconds()),
            "duration",
            task.getDuration(),
            "log",
            task.toString());

    systemSchema.getTable("Jobs").save(jobRow);
  }

  private void init() {
    if (!systemSchema.getTableNames().contains("Scripts")) {
      systemSchema.tx(
          db -> {
            Schema s = db.getSchema(systemSchema.getName());
            Table scripTypes = s.create(table("ScriptTypes").setTableType(TableType.ONTOLOGIES));
            Table jobStatus = s.create(table("JobStatus").setTableType(TableType.ONTOLOGIES));
            Table scripts =
                s.create(
                    table(
                        "Scripts",
                        column("name").setPkey(),
                        column("type").setType(ColumnType.ONTOLOGY).setRefTable("ScriptTypes"),
                        column("script").setType(ColumnType.TEXT),
                        column("parameters").setType(ColumnType.TEXT),
                        column("active")
                            .setType(ColumnType.BOOL)
                            .setDescription("Set to false to disable the script"),
                        column("cron")
                            .setDescription(
                                "If you want to run this script regularly you can add a cron expression. Cron expression. A cron expression is a string comprised of 6 or 7 fields separated by white space. These fields are: Seconds, Minutes, Hours, Day of month, Month, Day of week, and optionally Year. An example input is 0 0 12 * * ? for a job that fires at noon every day. See http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-06.html")));
            Table jobs =
                s.create(
                    table(
                        "Jobs",
                        column("id").setPkey(),
                        column("description").setType(ColumnType.TEXT),
                        column("script")
                            .setType(ColumnType.REF)
                            .setRefTable("Scripts")
                            .setDescription("Optional, only for script jobs"),
                        column("submission").setType(ColumnType.DATETIME),
                        column("start").setType(ColumnType.DATETIME),
                        column("status").setType(ColumnType.ONTOLOGY).setRefTable("JobStatus"),
                        column("duration")
                            .setType(ColumnType.INT)
                            .setDescription("Duration in milliseconds"),
                        column("log")
                            .setType(ColumnType.TEXT)
                            .setDescription("Log in JSON task serialization format")));
            // import the codes
            scripTypes.insert(row("name", "Python"));
            jobStatus.insert(
                Arrays.stream(TaskStatus.values()).map(value -> row("name", value)).toList());
          });
    } // else, migrations in the future

    // check that there are no 'waiting' or 'running' tasks from before server restarted
    Table jobsTable = systemSchema.getTable("Jobs");
    List<Row> faultyJobRows =
        jobsTable
            .where(
                f(
                    "status",
                    or(
                        f("name", Operator.EQUALS, TaskStatus.WAITING),
                        f("name", Operator.EQUALS, TaskStatus.RUNNING))))
            .retrieveRows()
            .stream()
            .map(
                row -> {
                  row.set("status", TaskStatus.ERROR);
                  row.set(
                      "result",
                      String.format(
                          "{'description':'%s failed because of restart'}",
                          row.getString("description")));
                  return row;
                })
            .collect(Collectors.toList());
    jobsTable.save(faultyJobRows);

    // todo reload the scheduled jobs to be managed
  }
}
