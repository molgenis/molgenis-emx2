package org.molgenis.emx2.io.submission;

import static org.molgenis.emx2.ColumnType.REFBACK;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.FilterBean.*;
import static org.molgenis.emx2.Operator.EQUALS;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.tasks.Task;

/**
 * Will create a schema for user to enter a new data submission based on a configuration of the
 * submission
 */
public class SubmissionCreateTask extends Task {
  private SubmissionRecord submissionRecord;
  private SubmissionService submissionService;
  private ObjectMapper objectMapper = new ObjectMapper();

  public SubmissionCreateTask(
      SubmissionService submissionService, SubmissionRecord submissionRecord) {
    super("Creating submission");
    this.submissionService = submissionService;
    this.submissionRecord = submissionRecord;
  }

  @Override
  public void run() {
    this.start();
    try {
      submissionService
          .getDatabase()
          .txAsAdmin(
              db -> {
                Schema targetSchema = db.getSchema(submissionRecord.getTargetSchema());

                // copy the table definitions into new schema
                // correcting xrefs to 'local' schema where needed
                SchemaMetadata draftSchema = new SchemaMetadata();
                Set<String> targetTableNames =
                    getTableNamesIncludingInherited(
                        targetSchema, submissionRecord.getTargetTables());
                copyTableMetadataIntoDraftSchemaWhileFixingReferencesToTargetSchema(
                    targetSchema, draftSchema, targetTableNames);

                // enable track changes
                String rawSetting = db.getSetting("CHANGELOG_SCHEMAS");

                List<String> changelogSetting =
                    rawSetting != null
                        ? new ArrayList<>(List.of(rawSetting.split(",")))
                        : new ArrayList<>();
                changelogSetting.add(submissionRecord.getSchema());
                db.setSetting("CHANGELOG_SCHEMAS", String.join(",", changelogSetting));

                // create the schema
                Task subtask = this.addSubTask("Creating submission workspace").start();
                Schema schema = db.createSchema(submissionRecord.getSchema());
                schema.migrate(draftSchema);
                subtask.complete();

                // load the submission data to be updated, if selected
                if (this.getSubmissionRecord().getTargetIdentifiers() != null) {
                  loadSelectedIdentifiersForUpdateSubmissions(targetSchema, schema);
                }

                // now our submission is ready to be edited, lets save this state
                this.submissionRecord.setStatus(SubmissionRecord.SubmissionStatus.DRAFT);
                this.submissionService.save(submissionRecord);
              });
      this.addSubTask("Complete").complete();
      this.complete();
    } catch (Exception e) {
      this.setError("Create submission schema failed: " + e.getMessage());
      this.submissionRecord.setStatus(SubmissionRecord.SubmissionStatus.ERROR);
      this.submissionService.save(submissionRecord);
    }
  }

  private void loadSelectedIdentifiersForUpdateSubmissions(
      Schema targetSchema, Schema submissionSchema) {
    List<String> tables = submissionRecord.getTargetTables();
    Set<String> tablesInclInherited = this.getTableNamesIncludingInherited(targetSchema, tables);
    Table mainTable = submissionSchema.getTable(tables.get(0));
    List<String> mainTableAndInherited = getInheritedTables(mainTable);
    try {
      Map<String, Object> key =
          objectMapper.readValue(submissionRecord.getTargetIdentifiers(), Map.class);
      Filter keyFilter = createKeyFilter(key);

      // first load record from main table
      Task loadTask = this.addSubTask("Loading data for " + mainTable.getName()).start();
      List<Row> rows =
          targetSchema.getTable(mainTable.getName()).query().where(keyFilter).retrieveRows();
      rows.forEach(row -> row.getValueMap().remove(MG_TABLECLASS));
      int count = mainTable.save(rows);
      loadTask.complete(String.format("Loaded %s %s", count, mainTable.getName()));

      // get the refs if in same schema
      // analyse: do we also need to do this for other tables?
      // easier with json, still quite some heavy lifting
      List<Map<String, ?>> result =
          (List)
              objectMapper
                  .readValue(
                      targetSchema
                          .getTable(mainTable.getName())
                          .query()
                          .where(keyFilter)
                          .retrieveJSON(),
                      Map.class)
                  .get(mainTable.getName());
      mainTable
          .getMetadata()
          .getColumns()
          .forEach(
              column -> {
                if (column.isReference()
                    && !column.isRefback()
                    && tablesInclInherited.contains(column.getRefTableName())) {
                  Task refTask =
                      this.addSubTask(
                              "Loading records for " + mainTable.getName() + "." + column.getName())
                          .start();
                  Table targetTable = targetSchema.getTable(column.getRefTableName());
                  Table submissionTable = submissionSchema.getTable(column.getRefTableName());
                  List<Filter> filters = new ArrayList<>();
                  if (column.isRefArray()) {
                    result.forEach(
                        object -> {
                          if (object.get(column.getName()) != null) {
                            ((Map<String, List>) object)
                                .get(column.getName())
                                .forEach(
                                    objectKey -> filters.add(createKeyFilter((Map) objectKey)));
                          }
                        });
                  } else {
                    result.forEach(
                        object -> {
                          if (object.get(column.getName()) != null) {
                            filters.add(createKeyFilter((Map) object.get(column.getName())));
                          }
                        });
                  }
                  List<Row> refRows = targetTable.where(or(filters)).retrieveRows();
                  refRows.forEach(row -> row.getValueMap().remove(MG_TABLECLASS));
                  int refResult = submissionTable.save(refRows);
                  refTask.complete(
                      String.format(
                          "Loaded %s %s records for %s",
                          refResult,
                          submissionTable.getName(),
                          mainTable.getName() + "." + column.getName()));
                }
              });

      // then load data for all foreign keys into this
      for (String tableName : tables) {
        Table submissionTable = submissionSchema.getTable(tableName);
        Table targetTable = targetSchema.getTable(tableName);
        submissionTable
            .getMetadata()
            .getColumns()
            .forEach(
                column -> {
                  if (column.getRefTableName() != null
                      // should be a reference
                      && column.getRefSchema().equals(submissionSchema.getName())
                      // within current schema
                      && mainTableAndInherited.contains(column.getRefTableName())
                      // linking to our main table (or superclass)
                      && !column.isRefback()
                  // not refback
                  ) {
                    Task refTask = this.addSubTask("Loading data for " + mainTable).start();

                    // this is magic
                    List<Row> refRows =
                        targetTable.query().where(f(column.getName(), keyFilter)).retrieveRows();
                    refRows.forEach(row -> row.getValueMap().remove(MG_TABLECLASS));
                    int refCount = submissionTable.save(refRows);
                    refTask.complete(
                        String.format("Loaded %s %s", refCount, submissionTable.getName()));
                  }
                });
      }

    } catch (Exception e) {
      throw new MolgenisException("Failed to read identifiers", e);
    }
  }

  private void copyTableMetadataIntoDraftSchemaWhileFixingReferencesToTargetSchema(
      Schema targetSchema, SchemaMetadata draftSchema, Set<String> targetTableNames) {

    for (String tableName : targetTableNames) {
      Task subTask = this.addSubTask("Defining " + tableName).start();
      TableMetadata copy =
          new TableMetadata(draftSchema, targetSchema.getTable(tableName).getMetadata());

      for (Column c : copy.getColumns()) {
        //  we must make sure to reroute all foreign keys to targetSchema
        //  unless also a submission table
        if (c.getRefTableName() != null
            && c.getRefSchema() == null // already external in target schema, nothing to change then
            && !targetTableNames.contains(c.getRefTableName())) {
          c.setRefSchema(targetSchema.getName());
          copy.alterColumn(c);
        }
        // remove refback unless also in the submission
        // because that is actually editing another table too
        if (REFBACK.equals(c.getColumnType()) && !targetTableNames.contains(c.getRefTableName())) {
          copy.dropColumn(c.getName());
        }
      }
      draftSchema.create(copy);
      subTask.complete("Defined " + tableName);
    }
  }

  private Set<String> getTableNamesIncludingInherited(
      Schema targetSchema, List<String> targetTables) {
    Set<String> inheritedTables = new HashSet<>();
    for (String tableName : targetTables) {
      if (!targetSchema.getTableNames().contains(tableName)) {
        throw new MolgenisException(
            String.format("config contained unknown table '%s'", tableName));
      }
      Table table = targetSchema.getTable(tableName);
      inheritedTables.addAll(getInheritedTables(table));
    }
    inheritedTables.addAll(targetTables);
    return inheritedTables;
  }

  private List<String> getInheritedTables(Table table) {
    List<String> inheritedTables = new ArrayList<>();
    inheritedTables.add(table.getName());
    Table inheritedTable = table.getInheritedTable();
    while (inheritedTable != null) {
      inheritedTables.add(inheritedTable.getName());
      inheritedTable = inheritedTable.getInheritedTable();
    }
    return inheritedTables;
  }

  // duplicate code from graphql package
  private Filter createKeyFilter(Map<String, Object> map) {
    List<Filter> result = new ArrayList<>();
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof Map) {
        result.add(f(entry.getKey(), createKeyFilter((Map<String, Object>) entry.getValue())));
      } else {
        result.add(f(entry.getKey(), EQUALS, entry.getValue()));
      }
    }
    return and(result);
  }

  public SubmissionRecord getSubmissionRecord() {
    return this.submissionRecord;
  }
}
