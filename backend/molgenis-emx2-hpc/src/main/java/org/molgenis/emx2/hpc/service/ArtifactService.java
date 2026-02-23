package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.LIKE;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;

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
      String name,
      String type,
      String format,
      String residence,
      String contentUrl,
      String metadata) {
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
                      "name", name,
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

  /** Lists files belonging to an artifact with optional prefix filter and pagination. */
  public List<Row> listFiles(String artifactId, String prefix, int limit, int offset) {
    List<Row>[] result = new List[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Table table = db.getSchema(systemSchemaName).getTable("HpcArtifactFiles");
          var query = table.where(f("artifact_id", EQUALS, artifactId));
          if (prefix != null && !prefix.isBlank()) {
            query = query.where(f("path", LIKE, prefix + "%"));
          }
          List<Row> allRows = query.retrieveRows();

          // Apply offset and limit
          int start = Math.min(offset, allRows.size());
          int end = Math.min(start + limit, allRows.size());
          result[0] = allRows.subList(start, end);
        });
    return result[0];
  }

  /** Counts files belonging to an artifact with optional prefix filter. */
  public int countFiles(String artifactId, String prefix) {
    int[] count = new int[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Table table = db.getSchema(systemSchemaName).getTable("HpcArtifactFiles");
          var query = table.where(f("artifact_id", EQUALS, artifactId));
          if (prefix != null && !prefix.isBlank()) {
            query = query.where(f("path", LIKE, prefix + "%"));
          }
          count[0] = query.retrieveRows().size();
        });
    return count[0];
  }

  /**
   * Gets a file with its binary content by artifact ID and path. Uses explicit FILE column
   * selection to load binary data.
   */
  public Row getFileWithContent(String artifactId, String path) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Table table = db.getSchema(systemSchemaName).getTable("HpcArtifactFiles");
          List<Row> rows =
              table
                  .query()
                  .select(
                      s("id"),
                      s("artifact_id"),
                      s("path"),
                      s("role"),
                      s("sha256"),
                      s("size_bytes"),
                      s("content_type"),
                      s("content", s("contents"), s("mimetype"), s("filename")))
                  .where(f("artifact_id", EQUALS, artifactId))
                  .where(f("path", EQUALS, path))
                  .retrieveRows();
          if (!rows.isEmpty()) {
            result[0] = rows.getFirst();
          }
        });
    return result[0];
  }

  /** Gets file metadata (without binary content) by artifact ID and path. */
  public Row getFileMetadata(String artifactId, String path) {
    Row[] result = new Row[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable("HpcArtifactFiles")
                  .where(f("artifact_id", EQUALS, artifactId))
                  .where(f("path", EQUALS, path))
                  .retrieveRows();
          if (!rows.isEmpty()) {
            result[0] = rows.getFirst();
          }
        });
    return result[0];
  }

  /**
   * Uploads a file by artifact ID and path. Upserts: if a file already exists at that path, updates
   * it; otherwise inserts. Transitions artifact CREATED→UPLOADING on first upload.
   *
   * @return the file ID
   */
  public String uploadFileByPath(
      String artifactId,
      String path,
      String role,
      String sha256,
      Long sizeBytes,
      String contentType,
      BinaryFileWrapper content) {
    String[] fileId = new String[1];
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

          // Check if file already exists at this path
          Table filesTable = schema.getTable("HpcArtifactFiles");
          List<Row> existing =
              filesTable
                  .where(f("artifact_id", EQUALS, artifactId))
                  .where(f("path", EQUALS, path))
                  .retrieveRows();

          if (!existing.isEmpty()) {
            // Update existing file
            Row fileRow = existing.getFirst();
            fileRow.set("role", role);
            fileRow.set("sha256", sha256);
            fileRow.set("size_bytes", sizeBytes);
            fileRow.set("content_type", contentType);
            if (content != null) {
              fileRow.set("content", content);
            }
            filesTable.update(fileRow);
            fileId[0] = fileRow.getString("id");
          } else {
            // Insert new file
            String newId = UUID.randomUUID().toString();
            Row fileRow =
                row(
                    "id", newId,
                    "artifact_id", artifactId,
                    "path", path,
                    "role", role,
                    "sha256", sha256,
                    "size_bytes", sizeBytes,
                    "content_type", contentType);
            if (content != null) {
              fileRow.set("content", content);
            }
            filesTable.insert(fileRow);
            fileId[0] = newId;
          }
        });
    return fileId[0];
  }

  /**
   * Deletes a file by artifact ID and path. Only allowed when artifact status is not COMMITTED.
   *
   * @return true if deleted, false if not found
   * @throws MolgenisException if artifact is committed
   */
  public boolean deleteFile(String artifactId, String path) {
    boolean[] deleted = new boolean[1];
    database.tx(
        db -> {
          db.becomeAdmin();
          Schema schema = db.getSchema(systemSchemaName);

          // Check artifact status
          Table artifactsTable = schema.getTable("HpcArtifacts");
          List<Row> artifacts = artifactsTable.where(f("id", EQUALS, artifactId)).retrieveRows();
          if (artifacts.isEmpty()) {
            return;
          }
          String status = artifacts.getFirst().getString("status");
          if (ArtifactStatus.COMMITTED.name().equals(status)) {
            throw new MolgenisException("Cannot delete files from committed artifact");
          }

          // Find and delete the file
          Table filesTable = schema.getTable("HpcArtifactFiles");
          List<Row> files =
              filesTable
                  .where(f("artifact_id", EQUALS, artifactId))
                  .where(f("path", EQUALS, path))
                  .retrieveRows();
          if (!files.isEmpty()) {
            filesTable.delete(files.getFirst());
            deleted[0] = true;
          }
        });
    return deleted[0];
  }
}
