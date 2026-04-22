package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.hpc.HpcFields.*;

import java.util.Arrays;
import org.molgenis.emx2.*;
import org.molgenis.emx2.hpc.HpcTables;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.model.HpcJobStatus;
import org.molgenis.emx2.sql.SqlDatabase;

/**
 * Creates HPC tables in the _SYSTEM_ schema. Follows the pattern from {@link
 * org.molgenis.emx2.tasks.TaskServiceInDatabase#init()}: runs inside db.tx(), uses
 * db.becomeAdmin(), and creates tables with the EMX2 table()/column() builders.
 *
 * <p>Tables are prefixed with "Hpc" to avoid collisions with existing system tables.
 */
public final class HpcSchemaInitializer {

  private HpcSchemaInitializer() {}

  /** Initializes all HPC tables if they don't already exist. Idempotent. */
  public static void init(SqlDatabase database, String systemSchemaName) {
    database.tx(
        db -> {
          db.becomeAdmin();

          Schema schema;
          if (!db.hasSchema(systemSchemaName)) {
            schema = db.createSchema(systemSchemaName);
          } else {
            schema = db.getSchema(systemSchemaName);
          }

          // Existing deployment: apply additive schema upgrades in place.
          if (schema.getTableNames().contains(HpcTables.JOBS)) {
            applySchemaUpgrades(schema);
            return;
          }

          // --- Ontology tables ---

          Table jobStatusTable =
              schema.create(table(HpcTables.JOB_STATUS).setTableType(TableType.ONTOLOGIES));
          jobStatusTable.insert(
              Arrays.stream(HpcJobStatus.values()).map(s -> row(NAME, s.name())).toList());

          Table artifactStatusTable =
              schema.create(table(HpcTables.ARTIFACT_STATUS).setTableType(TableType.ONTOLOGIES));
          artifactStatusTable.insert(
              Arrays.stream(ArtifactStatus.values()).map(s -> row(NAME, s.name())).toList());

          Table artifactResidenceTable =
              schema.create(table(HpcTables.ARTIFACT_RESIDENCE).setTableType(TableType.ONTOLOGIES));
          artifactResidenceTable.insert(
              row(NAME, "managed"),
              row(NAME, "posix"),
              row(NAME, "s3"),
              row(NAME, "http"),
              row(NAME, "reference"));

          // --- Data tables ---

          schema.create(
              table(
                  HpcTables.WORKERS,
                  column(WORKER_ID).setPkey(),
                  column(HOSTNAME),
                  column(REGISTERED_AT).setType(ColumnType.DATETIME),
                  column(LAST_HEARTBEAT_AT).setType(ColumnType.DATETIME)));

          schema.create(
              table(
                  HpcTables.WORKER_CAPABILITIES,
                  column(ID).setPkey(),
                  column(WORKER_ID)
                      .setType(ColumnType.REF)
                      .setRefTable(HpcTables.WORKERS)
                      .setRequired(true),
                  column(PROCESSOR).setRequired(true),
                  column(PROFILE).setRequired(true),
                  column(MAX_CONCURRENT_JOBS).setType(ColumnType.INT)));

          createCredentialStatusOntologyIfMissing(schema);
          createWorkerCredentialsTableIfMissing(schema);

          schema.create(
              table(
                  HpcTables.JOBS,
                  column(ID).setPkey(),
                  column(PROCESSOR).setRequired(true),
                  column(PROFILE),
                  column(PARAMETERS).setType(ColumnType.JSON),
                  column(STATUS)
                      .setType(ColumnType.ONTOLOGY)
                      .setRefTable(HpcTables.JOB_STATUS)
                      .setRequired(true),
                  column(WORKER_ID).setType(ColumnType.REF).setRefTable(HpcTables.WORKERS),
                  column(INPUTS).setType(ColumnType.JSON),
                  column(SLURM_JOB_ID),
                  column(SUBMIT_USER),
                  column(CREATED_AT).setType(ColumnType.DATETIME),
                  column(CLAIMED_AT).setType(ColumnType.DATETIME),
                  column(SUBMITTED_AT).setType(ColumnType.DATETIME),
                  column(STARTED_AT).setType(ColumnType.DATETIME),
                  column(COMPLETED_AT).setType(ColumnType.DATETIME),
                  column(PHASE),
                  column(MESSAGE).setType(ColumnType.TEXT),
                  column(PROGRESS).setType(ColumnType.DECIMAL),
                  column(TIMEOUT_SECONDS).setType(ColumnType.INT)));

          schema.create(
              table(
                  HpcTables.JOB_TRANSITIONS,
                  column(ID).setPkey(),
                  column(JOB_ID)
                      .setType(ColumnType.REF)
                      .setRefTable(HpcTables.JOBS)
                      .setRequired(true),
                  column(FROM_STATUS),
                  column(TO_STATUS).setRequired(true),
                  column(TIMESTAMP).setType(ColumnType.DATETIME).setRequired(true),
                  column(WORKER_ID),
                  column(DETAIL).setType(ColumnType.TEXT),
                  column(PHASE),
                  column(MESSAGE).setType(ColumnType.TEXT),
                  column(PROGRESS).setType(ColumnType.DECIMAL)));

          schema.create(
              table(
                  HpcTables.ARTIFACTS,
                  column(ID).setPkey(),
                  column(NAME),
                  column(TYPE),
                  column(RESIDENCE)
                      .setType(ColumnType.ONTOLOGY)
                      .setRefTable(HpcTables.ARTIFACT_RESIDENCE),
                  column(STATUS)
                      .setType(ColumnType.ONTOLOGY)
                      .setRefTable(HpcTables.ARTIFACT_STATUS)
                      .setRequired(true),
                  column(SHA256),
                  column(SIZE_BYTES).setType(ColumnType.LONG),
                  column(CONTENT_URL),
                  column(METADATA).setType(ColumnType.JSON),
                  column("schema_info").setType(ColumnType.JSON),
                  column(CREATED_AT).setType(ColumnType.DATETIME),
                  column(COMMITTED_AT).setType(ColumnType.DATETIME)));

          schema.create(
              table(
                  HpcTables.ARTIFACT_FILES,
                  column(ID).setPkey(),
                  column(ARTIFACT_ID)
                      .setType(ColumnType.REF)
                      .setRefTable(HpcTables.ARTIFACTS)
                      .setRequired(true),
                  column(PATH).setRequired(true),
                  column(SHA256),
                  column(SIZE_BYTES).setType(ColumnType.LONG),
                  column("content").setType(ColumnType.FILE),
                  column(CONTENT_TYPE)));

          applySchemaUpgrades(schema);
        });
  }

  private static void applySchemaUpgrades(Schema schema) {
    if (!schema.getTableNames().contains(HpcTables.JOBS)) {
      return;
    }
    createCredentialStatusOntologyIfMissing(schema);
    createWorkerCredentialsTableIfMissing(schema);

    if (schema.getTableNames().contains(HpcTables.WORKER_CREDENTIALS)) {
      Table credentials = schema.getTable(HpcTables.WORKER_CREDENTIALS);
      addColumnIfMissing(credentials, column(WORKER_ID).setRequired(true));
      addColumnIfMissing(credentials, column(SECRET_ENCRYPTED).setType(ColumnType.TEXT));
      addColumnIfMissing(
          credentials,
          column(STATUS)
              .setType(ColumnType.ONTOLOGY)
              .setRefTable(HpcTables.WORKER_CREDENTIAL_STATUS)
              .setRequired(true));
      addColumnIfMissing(credentials, column(LABEL));
      addColumnIfMissing(credentials, column(CREATED_AT).setType(ColumnType.DATETIME));
      addColumnIfMissing(credentials, column(CREATED_BY));
      addColumnIfMissing(credentials, column(LAST_USED_AT).setType(ColumnType.DATETIME));
      addColumnIfMissing(credentials, column(REVOKED_AT).setType(ColumnType.DATETIME));
      addColumnIfMissing(credentials, column(EXPIRES_AT).setType(ColumnType.DATETIME));
    }

    if (schema.getTableNames().contains(HpcTables.ARTIFACTS)) {
      addColumnIfMissing(
          schema.getTable(HpcTables.JOBS),
          column(OUTPUT_ARTIFACT_ID).setType(ColumnType.REF).setRefTable(HpcTables.ARTIFACTS));
      addColumnIfMissing(
          schema.getTable(HpcTables.JOBS),
          column(LOG_ARTIFACT_ID).setType(ColumnType.REF).setRefTable(HpcTables.ARTIFACTS));
    }
    addColumnIfMissing(schema.getTable(HpcTables.JOBS), column(PHASE));
    addColumnIfMissing(schema.getTable(HpcTables.JOBS), column(MESSAGE).setType(ColumnType.TEXT));
    addColumnIfMissing(
        schema.getTable(HpcTables.JOBS), column(PROGRESS).setType(ColumnType.DECIMAL));

    if (schema.getTableNames().contains(HpcTables.JOB_TRANSITIONS)) {
      addColumnIfMissing(schema.getTable(HpcTables.JOB_TRANSITIONS), column(PHASE));
      addColumnIfMissing(
          schema.getTable(HpcTables.JOB_TRANSITIONS), column(MESSAGE).setType(ColumnType.TEXT));
      addColumnIfMissing(
          schema.getTable(HpcTables.JOB_TRANSITIONS), column(PROGRESS).setType(ColumnType.DECIMAL));
    }
  }

  private static void createCredentialStatusOntologyIfMissing(Schema schema) {
    if (schema.getTableNames().contains(HpcTables.WORKER_CREDENTIAL_STATUS)) {
      return;
    }
    Table credentialStatusTable =
        schema.create(table(HpcTables.WORKER_CREDENTIAL_STATUS).setTableType(TableType.ONTOLOGIES));
    credentialStatusTable.insert(row(NAME, "ACTIVE"), row(NAME, "REVOKED"), row(NAME, "EXPIRED"));
  }

  private static void createWorkerCredentialsTableIfMissing(Schema schema) {
    if (schema.getTableNames().contains(HpcTables.WORKER_CREDENTIALS)) {
      return;
    }
    schema.create(
        table(
            HpcTables.WORKER_CREDENTIALS,
            column(ID).setPkey(),
            column(WORKER_ID).setRequired(true),
            column(SECRET_ENCRYPTED).setType(ColumnType.TEXT),
            column(STATUS)
                .setType(ColumnType.ONTOLOGY)
                .setRefTable(HpcTables.WORKER_CREDENTIAL_STATUS)
                .setRequired(true),
            column(LABEL),
            column(CREATED_AT).setType(ColumnType.DATETIME),
            column(CREATED_BY),
            column(LAST_USED_AT).setType(ColumnType.DATETIME),
            column(REVOKED_AT).setType(ColumnType.DATETIME),
            column(EXPIRES_AT).setType(ColumnType.DATETIME)));
  }

  private static void addColumnIfMissing(Table table, Column column) {
    if (!table.getMetadata().getColumnNames().contains(column.getName())) {
      table.getMetadata().add(column);
    }
  }
}
