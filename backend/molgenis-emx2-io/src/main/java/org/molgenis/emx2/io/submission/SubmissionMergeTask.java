package org.molgenis.emx2.io.submission;

import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.io.submission.SubmissionRecord.SubmissionStatus.MERGED;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.Task;
import org.molgenis.emx2.tasks.TaskStatus;

/**
 * Will create a schema for user to enter a new data submission based on a configuration of the
 * submission
 */
public class SubmissionMergeTask extends Task {
  SubmissionService submissionService;
  SubmissionRecord submissionRecord;

  public SubmissionMergeTask(
      SubmissionService submissionService, SubmissionRecord submissionRecord) {
    super("Merging submission into ");
    this.submissionService = submissionService;
    this.submissionRecord = submissionRecord;
  }

  @Override
  public void run() {
    // prep commit subtask for later
    Task commit = new Task("Committing changes", TaskStatus.RUNNING);
    try {
      submissionService
          .getDatabase()
          .tx(
              database -> {
                Schema targetSchema = database.getSchema(submissionRecord.getTargetSchema());
                Schema submissionSchema = database.getSchema(submissionRecord.getSchema());

                // get deletions from changelog
                // edge case: what if people create and then delete a record?
                ObjectMapper mapper = new ObjectMapper();
                Map<String, List<Row>> deletions = new LinkedHashMap<>();
                submissionSchema.getChanges(Integer.MAX_VALUE).stream()
                    .filter(change -> change.operation() == 'D')
                    .forEach(
                        change -> {
                          if (!deletions.containsKey(change.tableName())) {
                            deletions.put(change.tableName(), new ArrayList<>());
                          }
                          try {
                            deletions
                                .get(change.tableName())
                                .add(new Row(mapper.readValue(change.oldRowData(), Map.class)));
                          } catch (Exception e) {
                            throw new MolgenisException("Couldn't read change log", e);
                          }
                        });

                // copying all new and updated data
                for (Table targetTable : targetSchema.getTablesSorted()) {
                  // if exist in submission schema
                  if (submissionRecord.getTargetTables().contains(targetTable.getName())) {
                    Table submissionTable = submissionSchema.getTable(targetTable.getName());

                    // delete rows
                    if (deletions.containsKey(targetTable.getName())) {
                      Task deleteTask =
                          this.addSubTask(
                                  String.format("Deleting rows from %s", targetTable.getName()))
                              .start();
                      int result = targetTable.delete(deletions.get(targetTable.getName()));
                      deleteTask
                          .setDescription(
                              String.format(
                                  "Deleted %s rows from %s", result, targetTable.getName()))
                          .complete()
                          .setStatus(TaskStatus.WARNING);
                    }

                    // copy rows
                    List<Row> rows =
                        submissionTable.retrieveRows(); // we assume submissions to be <10k rows
                    // is this a bug or a feature
                    // but we must remove tableclass otherwise we update the wrong schema
                    rows.forEach(row -> row.getValueMap().remove(MG_TABLECLASS));
                    int result = targetTable.save(rows);
                    this.addSubTask(
                            String.format("Copied %s rows from %s", result, targetTable.getName()))
                        .complete();
                  }
                }

                // commiting, may take longest time so have it in running state
                this.addSubTask(commit);
              });
      // merge success, save the state
      submissionRecord.setStatus(MERGED);
      submissionService.save(submissionRecord);
      commit.complete();
      this.complete();
    } catch (Exception me) {
      commit.setError();
      this.setError("Merge submission failed");
    }
  }
}
