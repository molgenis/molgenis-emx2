package org.molgenis.emx2.tasks;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.FilterBean.or;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.utils.TypeUtils.millisecondsToLocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.SqlDatabase;

public class TaskServiceInDatabase extends TaskServiceInMemory {
  private Database database;
  private String systemSchemaName;

  public TaskServiceInDatabase(String systemSchemaName) {
    this.database = new SqlDatabase(false);
    this.systemSchemaName = systemSchemaName;
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
    Task task = super.getTask(id);
    if (task == null) {
      return getTaskFromDatabase(id);
    }
    return task;
  }

  public Task getTaskFromDatabase(String id) {
    final StringBuilder json = new StringBuilder();
    database.tx(
        db -> {
          db.becomeAdmin();
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable("Jobs")
                  .where(f("id", EQUALS, id))
                  .retrieveRows();
          if (rows.size() > 0) {
            Row jobRow = rows.get(0);
            json.append(jobRow.getString("log"));
          }
        });
    try {
      return (new ObjectMapper().readValue(json.toString(), Task.class));
    } catch (Exception e) {
      throw new MolgenisException("getTask(" + id + ") failed", e);
    }
  }

  @Override
  public String submitTaskFromName(final String scriptName, final String parameters) {
    StringBuilder result = new StringBuilder();
    String defaultUser = database.getActiveUser();
    database.tx(
        db -> {
          db.becomeAdmin();
          Schema systemSchema = db.getSchema(this.systemSchemaName);
          // retrieve the script from database
          List<Row> rows =
              systemSchema.getTable("Scripts").where(f("name", EQUALS, scriptName)).retrieveRows();
          if (rows.size() != 1) {
            throw new MolgenisException("Script " + scriptName + " not found");
          }
          Row scriptMetadata = rows.get(0);
          String user = scriptMetadata.getString("cronUser");
          if (user == null) {
            user = defaultUser;
          }
          db.setActiveUser(user);
          if (scriptMetadata != null) {
            if (scriptMetadata.getBoolean("disable") != null
                && scriptMetadata.getBoolean("disable")) {
              throw new MolgenisException("Script " + scriptName + " is disabled");
            }
            // submit the script
            result.append(
                this.submit(
                    new ScriptTask(scriptMetadata)
                        .parameters(parameters)
                        .token(
                            JWTgenerator.createTemporaryToken(
                                systemSchema.getDatabase(),
                                systemSchema.getDatabase().getActiveUser()))
                        .submitUser(user)));
          } else {
            throw new MolgenisException("Script execution failed: " + scriptName + " not found");
          }
        });
    return result.toString();
  }

  private void save(Task task) {
    this.saveWithOutput(task, null);
  }

  private void saveWithOutput(Task task, File outputFile) {
    Row jobRow = getRowFromTask(task);

    // in case of script we have some more info to store
    if (task instanceof ScriptTask scriptTask) {
      jobRow.set("script", scriptTask.getName());
      if (outputFile != null) {
        jobRow.set("output", new BinaryFileWrapper(outputFile));
      }
      jobRow.set("parameters", scriptTask.getParameters());
    }

    database.tx(
        db -> {
          db.becomeAdmin();
          if (db.getSchema(systemSchemaName)
              .getTable("Jobs")
              .where(f("id", EQUALS, jobRow.getString("id")))
              .retrieveRows()
              .isEmpty()) {
            db.getSchema(systemSchemaName).getTable("Jobs").insert(jobRow);
          } else {
            db.getSchema(systemSchemaName).getTable("Jobs").update(jobRow);
          }
        });
  }

  private Row getRowFromTask(Task task) {
    return row(
        "id",
        task.getId(),
        "status",
        task.getStatus(),
        "type",
        task.getClass().getSimpleName(),
        "description",
        task.getDescription(),
        "submitDate",
        millisecondsToLocalDateTime(task.getSubmitTimeMilliseconds()),
        "submitUser",
        task.getSubmitUser(),
        "startDate",
        millisecondsToLocalDateTime(task.getStartTimeMilliseconds()),
        "duration",
        task.getDuration(),
        "log",
        task.toString());
  }

  private void init() {
    this.database.tx(
        db -> {
          db.becomeAdmin();

          Schema schema = null;
          if (!db.hasSchema(this.systemSchemaName)) {
            schema = db.createSchema(this.systemSchemaName);
          } else {
            schema = db.getSchema(this.systemSchemaName);
          }

          if (!schema.getTableNames().contains("Scripts")) {

            Table scripTypes =
                schema.create(table("ScriptTypes").setTableType(TableType.ONTOLOGIES));
            Table jobStatus = schema.create(table("JobStatus").setTableType(TableType.ONTOLOGIES));
            Table scripts =
                schema.create(
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
                                "For python, this should match requirements format for 'pip install -r requirements.txt'"),
                        column("outputFileExtension")
                            .setDescription("Extension, without the '.'. E.g. 'txt' or 'json'"),
                        column("disabled")
                            .setType(ColumnType.BOOL)
                            .setDescription(
                                "Set true to disable the script, it will then not be executable"),
                        column("cron")
                            .setDescription(
                                "If you want to run this script regularly you can add a cron expression. Cron expression. A cron expression is a string comprised of 6 or 7 fields separated by white space. These fields are: Seconds, Minutes, Hours, Day of month, Month, Day of week, and optionally Year. Use * for any and ? for ignore.Note you cannot set 'day of week' and 'day of month' at same time (use ? for one of them). An example input is 0 0 12 * * ? for a job that fires at noon every day. See http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-06.html")));
            Table jobs =
                schema.create(
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
                        column("script").setDescription("Contents of the script that was run"),
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
import sys
# you can get parameters via sys.argv[1]
print('Hello, world!')
a = np.array([1, 2, 3, 4, 5, 6])
print("MOLGENIS_TOKEN="+os.environ['MOLGENIS_TOKEN']);
if len(sys.argv) >= 2:
    print("sys.argv[1]="+sys.argv[1]);
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
          } // else, migrations in the future

          // check that there are no 'waiting' or 'running' tasks from before server restarted
          Table jobsTable = schema.getTable("Jobs");
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

          schema
              .getMetadata()
              .setSetting(
                  "menu",
                  """
[{"label":"Tasks","href":"tasks","key":"t1yefr","submenu":[],"role":"Manager"},{"label":"Up/Download","href":"updownload","role":"Editor","key":"eq0fcp","submenu":[]},{"label":"Graphql","href":"graphql-playground","role":"Viewer","key":"bifta5","submenu":[]},{"label":"Settings","href":"settings","role":"Manager","key":"7rh3b8","submenu":[]},{"label":"Help","href":"docs","role":"Viewer","key":"gq6ixb","submenu":[]}]
""");

          // todo reload the scheduled jobs to be managed
        });
  }

  public Table getScriptTable() {
    return database.getSchema(systemSchemaName).getTable("Scripts");
  }

  public Table getJobTable() {
    return database.getSchema(systemSchemaName).getTable("Jobs");
  }
}
