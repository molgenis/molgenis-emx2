package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Arrays;
import org.molgenis.emx2.*;
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

          // Skip if already initialized (check for the main jobs table)
          if (schema.getTableNames().contains("HpcJobs")) {
            return;
          }

          // --- Ontology tables ---

          Table jobStatusTable =
              schema.create(table("HpcJobStatus").setTableType(TableType.ONTOLOGIES));
          jobStatusTable.insert(
              Arrays.stream(HpcJobStatus.values()).map(s -> row("name", s.name())).toList());

          Table artifactStatusTable =
              schema.create(table("HpcArtifactStatus").setTableType(TableType.ONTOLOGIES));
          artifactStatusTable.insert(
              Arrays.stream(ArtifactStatus.values()).map(s -> row("name", s.name())).toList());

          Table artifactResidenceTable =
              schema.create(table("HpcArtifactResidence").setTableType(TableType.ONTOLOGIES));
          artifactResidenceTable.insert(
              row("name", "managed"),
              row("name", "posix"),
              row("name", "s3"),
              row("name", "http"),
              row("name", "reference"));

          Table artifactTypeTable =
              schema.create(table("HpcArtifactType").setTableType(TableType.ONTOLOGIES));
          artifactTypeTable.insert(
              row("name", "tabular"),
              row("name", "model"),
              row("name", "dataset"),
              row("name", "log"),
              row("name", "report"),
              row("name", "container"),
              row("name", "blob"));

          // --- Data tables ---

          schema.create(
              table(
                  "HpcWorkers",
                  column("worker_id").setPkey(),
                  column("hostname"),
                  column("registered_at").setType(ColumnType.DATETIME),
                  column("last_heartbeat_at").setType(ColumnType.DATETIME)));

          schema.create(
              table(
                  "HpcWorkerCapabilities",
                  column("id").setPkey(),
                  column("worker_id")
                      .setType(ColumnType.REF)
                      .setRefTable("HpcWorkers")
                      .setRequired(true),
                  column("processor").setRequired(true),
                  column("profile").setRequired(true),
                  column("max_concurrent_jobs").setType(ColumnType.INT)));

          schema.create(
              table(
                  "HpcJobs",
                  column("id").setPkey(),
                  column("processor").setRequired(true),
                  column("profile"),
                  column("parameters").setType(ColumnType.TEXT),
                  column("status")
                      .setType(ColumnType.ONTOLOGY)
                      .setRefTable("HpcJobStatus")
                      .setRequired(true),
                  column("worker_id").setType(ColumnType.REF).setRefTable("HpcWorkers"),
                  column("inputs").setType(ColumnType.TEXT),
                  column("slurm_job_id"),
                  column("submit_user"),
                  column("created_at").setType(ColumnType.DATETIME),
                  column("claimed_at").setType(ColumnType.DATETIME),
                  column("submitted_at").setType(ColumnType.DATETIME),
                  column("started_at").setType(ColumnType.DATETIME),
                  column("completed_at").setType(ColumnType.DATETIME)));

          schema.create(
              table(
                  "HpcJobTransitions",
                  column("id").setPkey(),
                  column("job_id").setType(ColumnType.REF).setRefTable("HpcJobs").setRequired(true),
                  column("from_status"),
                  column("to_status").setRequired(true),
                  column("timestamp").setType(ColumnType.DATETIME).setRequired(true),
                  column("worker_id"),
                  column("detail").setType(ColumnType.TEXT)));

          schema.create(
              table(
                  "HpcArtifacts",
                  column("id").setPkey(),
                  column("type").setType(ColumnType.ONTOLOGY).setRefTable("HpcArtifactType"),
                  column("format"),
                  column("residence")
                      .setType(ColumnType.ONTOLOGY)
                      .setRefTable("HpcArtifactResidence"),
                  column("status")
                      .setType(ColumnType.ONTOLOGY)
                      .setRefTable("HpcArtifactStatus")
                      .setRequired(true),
                  column("sha256"),
                  column("size_bytes").setType(ColumnType.LONG),
                  column("content_url"),
                  column("metadata").setType(ColumnType.TEXT),
                  column("schema_info").setType(ColumnType.TEXT),
                  column("created_at").setType(ColumnType.DATETIME),
                  column("committed_at").setType(ColumnType.DATETIME)));

          schema.create(
              table(
                  "HpcArtifactFiles",
                  column("id").setPkey(),
                  column("artifact_id")
                      .setType(ColumnType.REF)
                      .setRefTable("HpcArtifacts")
                      .setRequired(true),
                  column("path").setRequired(true),
                  column("role"),
                  column("sha256"),
                  column("size_bytes").setType(ColumnType.LONG),
                  column("content").setType(ColumnType.FILE),
                  column("content_type")));
        });
  }
}
