package org.molgenis.emx2.hpc.service;

import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.LIKE;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.molgenis.emx2.*;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Artifact lifecycle: create, upload files, commit with SHA-256 verification. Supports two paths:
 *
 * <ul>
 *   <li>Managed upload: CREATED → UPLOADING (on first file upload) → COMMITTED (on commit)
 *   <li>External reference: REGISTERED → COMMITTED (for posix/s3/http artifacts)
 * </ul>
 */
public class ArtifactService {

  private static final Logger logger = LoggerFactory.getLogger(ArtifactService.class);

  /** Default stale timeout: 1 hour for artifacts stuck in CREATED or UPLOADING. */
  private static final long DEFAULT_STALE_TIMEOUT_SECONDS = 3600;

  /** Explicit FILE column selection to load binary data. */
  private static final SelectColumn FILE_CONTENT_SELECT =
      s("content", s("contents"), s("mimetype"), s("filename"));

  private final TxHelper tx;
  private final String systemSchemaName;

  public ArtifactService(SqlDatabase database, String systemSchemaName) {
    this.tx = new TxHelper(database);
    this.systemSchemaName = systemSchemaName;
  }

  /**
   * Creates a new artifact record. For managed uploads, starts in CREATED status. For external
   * references, starts in REGISTERED status.
   *
   * @return the artifact ID
   */
  public String createArtifact(
      String name, String type, String residence, String contentUrl, String metadata) {
    String artifactId = UUID.randomUUID().toString();
    boolean isExternal = residence != null && !"managed".equals(residence);
    ArtifactStatus initialStatus = isExternal ? ArtifactStatus.REGISTERED : ArtifactStatus.CREATED;

    tx.tx(
        db ->
            db.getSchema(systemSchemaName)
                .getTable("HpcArtifacts")
                .insert(
                    row(
                        "id", artifactId,
                        "name", name,
                        "type", type,
                        "residence", residence != null ? residence : "managed",
                        "status", initialStatus.name(),
                        "content_url", contentUrl,
                        "metadata", metadata,
                        "created_at", LocalDateTime.now())));
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
      String sha256,
      Long sizeBytes,
      String contentType,
      BinaryFileWrapper content) {
    String fileId = UUID.randomUUID().toString();
    tx.tx(
        db -> {
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
   * Commits an artifact with tree hash computation and verification. Computes the tree hash from
   * stored files, verifies against the client-provided hash (if any), and sets the final SHA-256
   * and size. Validates that the artifact is in a commitable state (UPLOADING or REGISTERED).
   *
   * @return a CommitResult indicating success, wrong state, or hash mismatch
   */
  public CommitResult commitArtifact(String artifactId, String sha256, Long sizeBytes) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table artifactsTable = schema.getTable("HpcArtifacts");

          List<Row> rows = artifactsTable.where(f("id", EQUALS, artifactId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row artifact = rows.getFirst();
          ArtifactStatus current = ArtifactStatus.valueOf(artifact.getString("status"));
          if (!current.canTransitionTo(ArtifactStatus.COMMITTED)) {
            return CommitResult.wrongState(
                "Artifact " + artifactId + " cannot be committed from status " + current.name());
          }

          // Compute tree hash from stored files
          List<Row> files =
              schema
                  .getTable("HpcArtifactFiles")
                  .where(f("artifact_id", EQUALS, artifactId))
                  .retrieveRows();

          String computedHash = computeTreeHash(files);
          long computedSize =
              files.stream().mapToLong(f -> parseLong(f.getString("size_bytes"))).sum();

          // Verify client-provided hash if present
          if (sha256 != null && computedHash != null && !sha256.equals(computedHash)) {
            return CommitResult.hashMismatch("client=" + sha256 + " computed=" + computedHash);
          }

          // Use computed values when client doesn't provide them
          String finalHash = sha256 != null ? sha256 : computedHash;
          long finalSize = sizeBytes != null ? sizeBytes : computedSize;

          artifact.set("status", ArtifactStatus.COMMITTED.name());
          artifact.set("sha256", finalHash);
          artifact.set("size_bytes", finalSize);
          artifact.set("committed_at", LocalDateTime.now());
          artifactsTable.update(artifact);
          return CommitResult.success(artifact);
        });
  }

  /**
   * Computes the tree hash for a set of artifact files. Single-file: SHA-256 of the file. Multi-
   * file: SHA-256 of concatenated "path:sha256_hex" strings, sorted by path.
   */
  static String computeTreeHash(List<Row> files) {
    if (files.isEmpty()) {
      return null;
    }

    List<Row> sorted =
        files.stream().sorted(Comparator.comparing(f -> f.getString("path"))).toList();

    if (sorted.size() == 1) {
      // Single-file: use the file's own sha256
      return sorted.getFirst().getString("sha256");
    }

    // Multi-file: tree hash
    StringBuilder sb = new StringBuilder();
    for (Row file : sorted) {
      sb.append(file.getString("path")).append(":").append(file.getString("sha256"));
    }

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  private static long parseLong(String value) {
    if (value == null) return 0;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  /**
   * Expires artifacts that have been stuck in CREATED or UPLOADING for longer than the stale
   * timeout. Transitions them to FAILED so they become eligible for garbage collection.
   */
  public void expireStaleArtifacts() {
    tx.tx(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table artifactsTable = schema.getTable("HpcArtifacts");
          LocalDateTime cutoff = LocalDateTime.now().minusSeconds(DEFAULT_STALE_TIMEOUT_SECONDS);

          for (String statusName :
              List.of(ArtifactStatus.CREATED.name(), ArtifactStatus.UPLOADING.name())) {
            List<Row> stale =
                artifactsTable.where(f("status", f("name", EQUALS, statusName))).retrieveRows();
            for (Row artifact : stale) {
              String createdAtStr = artifact.getString("created_at");
              if (createdAtStr == null) continue;
              LocalDateTime createdAt;
              try {
                createdAt = LocalDateTime.parse(createdAtStr);
              } catch (DateTimeParseException e) {
                logger.warn(
                    "Unparseable created_at '{}' for artifact {}",
                    createdAtStr,
                    artifact.getString("id"));
                continue;
              }
              if (createdAt.isBefore(cutoff)) {
                String artifactId = artifact.getString("id");
                logger.info(
                    "Expiring stale artifact {} (status={}, created_at={})",
                    artifactId,
                    statusName,
                    createdAtStr);
                artifact.set("status", ArtifactStatus.FAILED.name());
                artifactsTable.update(artifact);
              }
            }
          }
        });
  }

  /** Gets an artifact by ID. */
  public Row getArtifact(String artifactId) {
    return tx.txResult(
        db -> {
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable("HpcArtifacts")
                  .where(f("id", EQUALS, artifactId))
                  .retrieveRows();
          return rows.isEmpty() ? null : rows.getFirst();
        });
  }

  /** Lists files belonging to an artifact with optional prefix filter and pagination. */
  // TODO: Use DB-level LIMIT/OFFSET when EMX2 Query API supports it
  public List<Row> listFiles(String artifactId, String prefix, int limit, int offset) {
    return tx.txResult(
        db -> {
          Table table = db.getSchema(systemSchemaName).getTable("HpcArtifactFiles");
          var query = table.where(f("artifact_id", EQUALS, artifactId));
          if (prefix != null && !prefix.isBlank()) {
            query = query.where(f("path", LIKE, prefix + "%"));
          }
          List<Row> allRows = query.retrieveRows();

          // Apply offset and limit
          int start = Math.min(offset, allRows.size());
          int end = Math.min(start + limit, allRows.size());
          return allRows.subList(start, end);
        });
  }

  /** Counts files belonging to an artifact with optional prefix filter. */
  public int countFiles(String artifactId, String prefix) {
    return tx.txResult(
        db -> {
          Table table = db.getSchema(systemSchemaName).getTable("HpcArtifactFiles");
          var query = table.where(f("artifact_id", EQUALS, artifactId));
          if (prefix != null && !prefix.isBlank()) {
            query = query.where(f("path", LIKE, prefix + "%"));
          }
          return query.retrieveRows().size();
        });
  }

  /**
   * Gets a file with its binary content by artifact ID and path. Uses explicit FILE column
   * selection to load binary data.
   */
  public Row getFileWithContent(String artifactId, String path) {
    return tx.txResult(
        db -> {
          Table table = db.getSchema(systemSchemaName).getTable("HpcArtifactFiles");
          List<Row> rows =
              table
                  .query()
                  .select(
                      s("id"),
                      s("artifact_id"),
                      s("path"),
                      s("sha256"),
                      s("size_bytes"),
                      s("content_type"),
                      FILE_CONTENT_SELECT)
                  .where(f("artifact_id", EQUALS, artifactId))
                  .where(f("path", EQUALS, path))
                  .retrieveRows();
          return rows.isEmpty() ? null : rows.getFirst();
        });
  }

  /** Gets file metadata (without binary content) by artifact ID and path. */
  public Row getFileMetadata(String artifactId, String path) {
    return tx.txResult(
        db -> {
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable("HpcArtifactFiles")
                  .where(f("artifact_id", EQUALS, artifactId))
                  .where(f("path", EQUALS, path))
                  .retrieveRows();
          return rows.isEmpty() ? null : rows.getFirst();
        });
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
      String sha256,
      Long sizeBytes,
      String contentType,
      BinaryFileWrapper content) {
    return tx.txResult(
        db -> {
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
            fileRow.set("sha256", sha256);
            fileRow.set("size_bytes", sizeBytes);
            fileRow.set("content_type", contentType);
            if (content != null) {
              fileRow.set("content", content);
            }
            filesTable.update(fileRow);
            return fileRow.getString("id");
          } else {
            // Insert new file
            String newId = UUID.randomUUID().toString();
            Row fileRow =
                row(
                    "id", newId,
                    "artifact_id", artifactId,
                    "path", path,
                    "sha256", sha256,
                    "size_bytes", sizeBytes,
                    "content_type", contentType);
            if (content != null) {
              fileRow.set("content", content);
            }
            filesTable.insert(fileRow);
            return newId;
          }
        });
  }

  /**
   * Deletes a file by artifact ID and path. Only allowed when artifact status is not COMMITTED.
   *
   * @return true if deleted, false if not found
   * @throws MolgenisException if artifact is committed
   */
  public boolean deleteFile(String artifactId, String path) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);

          // Check artifact status
          Table artifactsTable = schema.getTable("HpcArtifacts");
          List<Row> artifacts = artifactsTable.where(f("id", EQUALS, artifactId)).retrieveRows();
          if (artifacts.isEmpty()) {
            return false;
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
            return true;
          }
          return false;
        });
  }
}
