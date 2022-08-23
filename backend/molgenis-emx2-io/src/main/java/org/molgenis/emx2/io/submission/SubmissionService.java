package org.molgenis.emx2.io.submission;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.TaskService;

public class SubmissionService {
  public static final String SUBMISSIONS = "Submissions";
  public static final String SYSTEM = "system";
  public static final String ID = "id";
  public static final String CONFIG = "config";
  public static final String STATUS = "status";
  public static final String CREATED = "created";
  public static final String CHANGED = "changed";
  public static final String TARGET_SCHEMA = "targetSchema";
  public static final String TARGET_TABLES = "targetTables";

  private Schema schema;
  private TaskService taskService;

  public SubmissionService(Schema schema, TaskService taskService) {
    this.schema = schema;
    this.taskService = taskService;
    this.createTablesMigrateIfNeeded();
  }

  public List<SubmissionRecord> list() {
    List<SubmissionRecord> result = new ArrayList<>();
    schema
        .getDatabase()
        .txAsAdmin(
            database -> {
              Table submissionTable = database.getSchema(SYSTEM).getTable(SUBMISSIONS);
              // todo, filter on submission users agains current user
              result.addAll(
                  submissionTable
                      .query()
                      .where(f(TARGET_SCHEMA, Operator.EQUALS, schema.getName()))
                      .orderBy(CHANGED, Order.DESC)
                      .retrieveRows()
                      .stream()
                      .map(row -> new SubmissionRecord(row))
                      .toList());
            });
    return result;
  }

  public SubmissionRecord getById(String id) {
    List<Row> result = new ArrayList<>();
    schema
        .getDatabase()
        .txAsAdmin(
            database ->
                result.addAll(
                    database
                        .getSchema(SYSTEM)
                        .getTable(SUBMISSIONS)
                        .query()
                        .where(f(ID, Operator.EQUALS, id))
                        .retrieveRows()));
    if (result.size() > 0) {
      return new SubmissionRecord(result.get(0));
    } else {
      throw new MolgenisException(String.format("Submission with id '%s' doesn't exist", id));
    }
  }

  public int delete(String id) {
    int[] result = {0};
    schema
        .getDatabase()
        .txAsAdmin(
            database -> {
              result[0] =
                  database
                      .getSchema(SYSTEM)
                      .getTable(SUBMISSIONS)
                      .delete(toRow(new SubmissionRecord().setId(id)));
            });
    return result[0];
  }

  public SubmissionCreateTask createSubmission(List<String> targetTables, String jsonIdentifier) {
    // returning the new task for execution in task managers
    SubmissionRecord submissionRecord =
        new SubmissionRecord()
            .setTargetSchema(this.schema.getName())
            .setTargetTables(targetTables)
            .setTargetIdentifiers(jsonIdentifier);
    this.save(submissionRecord);
    return (SubmissionCreateTask)
        taskService.submit(new SubmissionCreateTask(this, submissionRecord));
  }

  public SubmissionMergeTask mergeSubmission(String id) {
    return (SubmissionMergeTask) taskService.submit(new SubmissionMergeTask(this, getById(id)));
  }

  private void createTablesMigrateIfNeeded() {
    schema
        .getDatabase()
        .txAsAdmin(
            database -> {
              // create schema if needed
              Schema systemSchema = database.getSchema(SYSTEM);
              if (systemSchema == null) {
                systemSchema = database.createSchema(SYSTEM);
              }
              // create submission table if needed
              Table submissionsTable = systemSchema.getTable(SUBMISSIONS);
              if (submissionsTable == null) {
                submissionsTable = systemSchema.create(table(SUBMISSIONS, column(ID).setPkey()));
              }

              // if needed in the future: add migrations here
              TableMetadata tableMetadata = submissionsTable.getMetadata();
              tableMetadata.add(column(TARGET_SCHEMA).setType(ColumnType.STRING));
              tableMetadata.add(column(TARGET_TABLES).setType(ColumnType.STRING_ARRAY));
              tableMetadata.add(column(STATUS));
              tableMetadata.add(column(CREATED).setType(ColumnType.DATETIME));
              tableMetadata.add(column(CHANGED).setType(ColumnType.DATETIME));
            });
  }

  public int save(SubmissionRecord submissionRecord) {
    int[] result = {0};
    schema
        .getDatabase()
        .txAsAdmin(
            database -> {
              result[0] =
                  database.getSchema(SYSTEM).getTable(SUBMISSIONS).save(toRow(submissionRecord));
            });
    return result[0];
  }

  public Database getDatabase() {
    return this.schema.getDatabase();
  }

  // todo, in future we can generate these code
  public Row toRow(SubmissionRecord submissionRecord) {
    return row(
        ID,
        submissionRecord.getId(),
        TARGET_SCHEMA,
        submissionRecord.getTargetSchema(),
        TARGET_TABLES,
        submissionRecord.getTargetTables(),
        CREATED,
        submissionRecord.getCreated(),
        CHANGED,
        LocalDateTime.now(),
        STATUS,
        submissionRecord.getStatus());
  }
}
