package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.FilterBean.or;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;

public class TaskServiceInDatabase extends TaskServiceInMemory {
  private Schema systemSchema;

  public TaskServiceInDatabase(Schema systemSchema) {
    // for testing make parameterizable
    this.systemSchema = systemSchema;
    this.init();
  }

  public TaskServiceInDatabase() {
    // default uses ADMIN schema and dedicated database instance
    Database database = new SqlDatabase(false);
    database.becomeAdmin();
    if (!database.hasSchema("ADMIN")) {
      // assumes database is never changed by humans
      database.createSchema("ADMIN");
    }
    this.systemSchema = database.getSchema("ADMIN");
    this.init();
  }

  @Override
  public String submit(Task task) {
    // we insert task to database
    save(task);

    // and we add a change handler to write task status changes to database
    task.setChangedHandler(
        new TaskChangedHandler() {
          @Override
          public void handleChange(Task changedTask) {
            save(changedTask);
          }

          @Override
          public void handleOutputFile(Task changedTask, File outFile) {
            saveWithOutput(changedTask, outFile);
          }
        });
    return super.submit(task);
  }

  @Override
  public Task getTask(String id) {
    return super.getTask(id);
    // todo: try get from database if not in cache
  }

  @Override
  public String submitTaskFromName(String name, String userName, String token) {
    // retrieve the script from database
    Row scriptMetadata =
        systemSchema.getTable("Scripts").where(f("name", EQUALS, name)).retrieveRows().get(0);
    if (scriptMetadata != null) {
      if (scriptMetadata.getBoolean("disable") != null && scriptMetadata.getBoolean("disable")) {
        throw new MolgenisException("Script " + name + " is disabled");
      }
      // submit the script
      return this.submit(
          new ScriptTask()
              .name(name)
              .script(scriptMetadata.getString("script"))
              .parameters(scriptMetadata.getText("parameters"))
              .outputFileExtension(scriptMetadata.getString("outputFileExtension"))
              .dependencies(scriptMetadata.getString("dependencies"))
              .token(token)
              .submitUser(userName));
    } else {
      throw new MolgenisException("Script execution failed: " + name + " not found");
    }
  }

  private void save(Task task) {
    this.saveWithOutput(task, null);
  }

  private void saveWithOutput(Task task, File outputFile) {
    Row jobRow =
        row(
            "id",
            task.getId(),
            "status",
            task.getStatus(),
            "type",
            task.getClass().getSimpleName(),
            "description",
            task.getDescription(),
            "submitDate",
            toDateTime(task.getSubmitTimeMilliseconds()),
            "submitUser",
            task.getSubmitUser(),
            "startDate",
            toDateTime(task.getStartTimeMilliseconds()),
            "duration",
            task.getDuration(),
            "log",
            task.toString());

    // in case of script we have some more info to store
    if (task instanceof ScriptTask) {
      ScriptTask scriptTask = (ScriptTask) task;
      jobRow.set("script", scriptTask.getName());
      if (outputFile != null) {
        jobRow.set("output", new BinaryFileWrapper(outputFile));
      }
    }

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
                        column("type")
                            .setType(ColumnType.ONTOLOGY)
                            .setRefTable("ScriptTypes")
                            .setDefaultValue("python"),
                        column("script").setType(ColumnType.TEXT),
                        column("dependencies")
                            .setType(ColumnType.TEXT)
                            .setDescription(
                                "For python, this should match requirements format for 'pip install -r dependencies.txt'"),
                        column("outputFileExtension"),
                        column("disabled")
                            .setType(ColumnType.BOOL)
                            .setDescription("Set true to disable the script"),
                        column("cron")
                            .setDescription(
                                "If you want to run this script regularly you can add a cron expression. Cron expression. A cron expression is a string comprised of 6 or 7 fields separated by white space. These fields are: Seconds, Minutes, Hours, Day of month, Month, Day of week, and optionally Year. An example input is 0 0 12 * * ? for a job that fires at noon every day. See http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-06.html")));
            Table jobs =
                s.create(
                    table(
                        "Jobs",
                        column("id").setPkey(),
                        column("status").setType(ColumnType.ONTOLOGY).setRefTable("JobStatus"),
                        column("type").setDescription("Type of the task, typically its class"),
                        column("description")
                            .setType(ColumnType.TEXT)
                            .setDescription("As provided by the task"),
                        column("parameters")
                            .setType(ColumnType.TEXT)
                            .setDescription("As provided by user who started the script"),
                        column("script")
                            .setType(ColumnType.REF)
                            .setRefTable("Scripts")
                            .setDescription("Optional, only for script ScriptTasks"),
                        column("submitUser").setDescription("User that submitted the job"),
                        column("submitDate").setType(ColumnType.DATETIME),
                        column("startDate")
                            .setType(ColumnType.DATETIME)
                            .setDescription("When the job moved from submitted to running"),
                        column("duration")
                            .setType(ColumnType.INT)
                            .setDescription("Duration in milliseconds"),
                        column("log")
                            .setType(ColumnType.TEXT)
                            .setDescription("Log of task execution in JSON format"),
                        column("output")
                            .setType(ColumnType.FILE)
                            .setDescription(
                                "output of the script, if output extension != null and based on OUTPUT_FILE environment variable")));
            // import defaults
            String demoScript =
                """
import os;
import numpy as np
print('Hello, world!')
a = np.array([1, 2, 3, 4, 5, 6])
print("MOLGENIS_TOKEN="+os.environ['MOLGENIS_TOKEN']);
OUTPUT_FILE=os.environ['OUTPUT_FILE'];
print("OUTPUT_FILE="+os.environ['OUTPUT_FILE']);
f = open(OUTPUT_FILE, "a")
f.write("Readme")
f.close()
""";
            scripts.insert(
                row(
                    "name",
                    "hello world",
                    "script",
                    demoScript,
                    "dependencies",
                    "numpy==1.23.4", // it has a dependency :-)
                    "type",
                    "python",
                    "outputFileExtension",
                    "txt"));
            scripTypes.insert(row("name", "python")); // lowercase by convention
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

    systemSchema
        .getMetadata()
        .setSetting(
            "menu",
            """
[{"label":"Tasks","href":"tasks","key":"t1yefr","submenu":[],"role":"Manager"},{"label":"Up/Download","href":"updownload","role":"Editor","key":"eq0fcp","submenu":[]},{"label":"Graphql","href":"graphql-playground","role":"Viewer","key":"bifta5","submenu":[]},{"label":"Settings","href":"settings","role":"Manager","key":"7rh3b8","submenu":[]},{"label":"Help","href":"docs","role":"Viewer","key":"gq6ixb","submenu":[]}]
""");

    // todo reload the scheduled jobs to be managed
  }

  private LocalDateTime toDateTime(long milliseconds) {
    if (milliseconds > 0) {
      return LocalDateTime.ofInstant(Instant.ofEpochMilli(milliseconds), ZoneId.systemDefault());
    } else {
      return null;
    }
  }
}
