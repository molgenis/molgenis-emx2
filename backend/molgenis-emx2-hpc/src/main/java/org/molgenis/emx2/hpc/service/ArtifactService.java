package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Row.row;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.molgenis.emx2.*;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.sql.SqlDatabase;

/**
 * Artifact lifecycle: create, upload files, commit with SHA-256 verification. Supports two paths:
 *
 * <ul>
 *   <li>Managed upload: CREATED → UPLOADING (on first file upload) → COMMITTED (on commit)
 *   <li>External reference: REGISTERED → COMMITTED (for posix/s3/http artifacts)
 * </ul>
 */
public class ArtifactService {

  private final SqlDatabase database;
  private final String systemSchemaName;

  public ArtifactService(SqlDatabase database, String systemSchemaName) {
    this.database = database;
    this.systemSchemaName = systemSchemaName;
  }

  /**
   * Creates a new artifact record. For managed uploads, starts in CREATED status. For external
   * references, starts in REGISTERED status.
   *
   * @return the artifact ID
   */
  public String createArtifact(
      String type, String format, String residence, String contentUrl, String metadata) {
    String artifactId = UUID.randomUUID().toString();
    boolean isExternal = residence != null && !"managed".equals(residence);
    ArtifactStatus initialStatus = isExternal ? ArtifactStatus.REGISTERED : ArtifactStatus.CREATED;

    database.tx(
        db -> {
          db.becomeAdmin();
          db.getSchema(systemSchemaName)
              .getTable("HpcArtifacts")
              .insert(
                  row(
                      "id", artifactId,
                      "type", type,
                      "format", format,
                      "residence", residence != null ? residence : "managed",
                      "status", initialStatus.name(),
                      "content_url", contentUrl,
                      "metadata", metadata,
                      "created_at", LocalDateTime.now()));
        });
    return artifactId;
  }

  /**
   * Records a file upload for an artifact. Transitions the artifact from CREATED to UPLOADING on
   * the first file upload.
   *
   * @return the file ID
   */
  public String uploadFile(
      String artifactId,
      String path,
      String role,
      String sha256,
      Long sizeBytes,
      String contentType,
      BinaryFileWrapper content) {
    String fileId = UUID.randomUUID().toString();
    database.tx(
        db -> {
          db.becomeAdmin();
          Schema schema = db.getSchema(systemSchemaName);

          // Transition artifact to UPLOADING if currently CREATED
          Table artifactsTable = schema.getTable("HpcArtifacts");
          List<Row> artifacts = artifactsTable.where(f("id", EQUALS, artifactId)).retrieveRows();
          if (artifacts.isEmpty()) {
            throw new MolgenisException("Artifact " + artifactId + " not found");
          }
          Row artifact = artifacts.getFirst();
          String currentStatus = artifact.getString("status");
          if (ArtifactStatus.CREATED.name().equals(currentStatus)) {
            artifact.set("status", ArtifactStatus.UPLOADING.name());
            artifactsTable.update(artifact);
          }

          // Insert file record
          Row fileRow =
              row(
                  "id", fileId,
                  "artifact_id", artifactId,
                  "path", path,
                  "role", role,
                  "sha256", sha256,
                  "size_bytes", sizeBytes,
                  "content_type", contentType);
          if (content != null) {
            fileRow.set("content", content);
          }
          schema.getTable("HpcArtifactFiles").insert(fileRow);
        });
    return fileId;
  }

  /**
   * Commits an artifact, setting its final SHA-256 and size. Validates that the artifact is in a
   * commitable state (UPLOADING or REGISTERED).
   *
   * @return the updated artifact row, or null if the artifact cannot be committed
   */
  public Row commitArtifact(String artifactId, String sha256, Long sizeBytes) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Schema schema = db.getSchema(systemSchemaName);
          Table artifactsTable = schema.getTable("HpcArtifacts");

          List<Row> rows = artifactsTable.where(f("id", EQUALS, artifactId)).retrieveRows();
          if (rows.isEmpty()) {
            return;
          }
          Row artifact = rows.getFirst();
          ArtifactStatus current = ArtifactStatus.valueOf(artifact.getString("status"));
          if (!current.canTransitionTo(ArtifactStatus.COMMITTED)) {
            return; // cannot commit from this state
          }

          artifact.set("status", ArtifactStatus.COMMITTED.name());
          artifact.set("sha256", sha256);
          artifact.set("size_bytes", sizeBytes);
          artifact.set("committed_at", LocalDateTime.now());
          artifactsTable.update(artifact);
          result[0] = artifact;
        });
    return result[0];
  }

  /** Gets an artifact by ID. */
  public Row getArtifact(String artifactId) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable("HpcArtifacts")
                  .where(f("id", EQUALS, artifactId))
                  .retrieveRows();
          if (!rows.isEmpty()) {
            result[0] = rows.getFirst();
          }
        });
    return result[0];
  }

  /** Lists files belonging to an artifact. */
  public List<Row> listFiles(String artifactId) {
    List<Row>[] result = new List[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          result[0] =
              db.getSchema(systemSchemaName)
                  .getTable("HpcArtifactFiles")
                  .where(f("artifact_id", EQUALS, artifactId))
                  .retrieveRows();
        });
    return result[0];
  }
}
