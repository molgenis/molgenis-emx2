package org.molgenis.emx2.hpc.service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.Operator.LIKE;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.hpc.HpcFields.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.molgenis.emx2.*;
import org.molgenis.emx2.hpc.HpcTables;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.protocol.InputValidator;
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
      String name, String type, String residence, String contentUrl, Object metadata) {
    // Defense-in-depth: validate content_url even if API layer already did
    InputValidator.validateContentUrl(contentUrl, residence);

    String artifactId = UUID.randomUUID().toString();
    boolean isExternal = residence != null && !"managed".equals(residence);
    ArtifactStatus initialStatus = isExternal ? ArtifactStatus.REGISTERED : ArtifactStatus.CREATED;

    tx.tx(
        db ->
            db.getSchema(systemSchemaName)
                .getTable(HpcTables.ARTIFACTS)
                .insert(
                    row(
                        ID, artifactId,
                        NAME, name,
                        TYPE, type,
                        RESIDENCE, residence != null ? residence : "managed",
                        STATUS, initialStatus.name(),
                        CONTENT_URL, contentUrl,
                        METADATA, metadata,
                        CREATED_AT, LocalDateTime.now())));
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
    // Keep POST /files behavior aligned with PUT /files/{path}: re-uploading the same path
    // replaces metadata/content instead of creating duplicate rows.
    return uploadFileByPath(artifactId, path, sha256, sizeBytes, contentType, content);
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
          Table artifactsTable = schema.getTable(HpcTables.ARTIFACTS);

          List<Row> rows = artifactsTable.where(f(ID, EQUALS, artifactId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row artifact = rows.getFirst();
          ArtifactStatus current = ArtifactStatus.valueOf(artifact.getString(STATUS));
          if (!current.canTransitionTo(ArtifactStatus.COMMITTED)) {
            return CommitResult.wrongState(
                "Artifact " + artifactId + " cannot be committed from status " + current.name());
          }

          // Compute tree hash from stored files
          List<Row> files =
              schema
                  .getTable(HpcTables.ARTIFACT_FILES)
                  .where(f(ARTIFACT_ID, EQUALS, artifactId))
                  .retrieveRows();

          String computedHash = computeTreeHash(files);
          long computedSize =
              files.stream().mapToLong(f -> parseLong(f.getString(SIZE_BYTES))).sum();

          // Verify client-provided hash if present
          if (sha256 != null && computedHash != null && !sha256.equals(computedHash)) {
            return CommitResult.hashMismatch("client=" + sha256 + " computed=" + computedHash);
          }

          // Use computed values when client doesn't provide them
          String finalHash = sha256 != null ? sha256 : computedHash;
          long finalSize = sizeBytes != null ? sizeBytes : computedSize;

          artifact.set(STATUS, ArtifactStatus.COMMITTED.name());
          artifact.set(SHA256, finalHash);
          artifact.set(SIZE_BYTES, finalSize);
          artifact.set(COMMITTED_AT, LocalDateTime.now());
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

    List<Row> sorted = files.stream().sorted(Comparator.comparing(f -> f.getString(PATH))).toList();

    if (sorted.size() == 1) {
      // Single-file: use the file's own sha256
      return sorted.getFirst().getString(SHA256);
    }

    // Multi-file: tree hash
    StringBuilder sb = new StringBuilder();
    for (Row file : sorted) {
      sb.append(file.getString(PATH)).append(":").append(file.getString(SHA256));
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
   * Deletes an artifact, its files, and nullifies any job references to it. Returns the artifact
   * row if deleted, null if not found.
   */
  public Row deleteArtifact(String artifactId) {
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);
          Table artifactsTable = schema.getTable(HpcTables.ARTIFACTS);

          List<Row> rows = artifactsTable.where(f(ID, EQUALS, artifactId)).retrieveRows();
          if (rows.isEmpty()) {
            return null;
          }
          Row artifact = rows.getFirst();

          // Delete files first (FK dependency)
          Table filesTable = schema.getTable(HpcTables.ARTIFACT_FILES);
          List<Row> files = filesTable.where(f(ARTIFACT_ID, EQUALS, artifactId)).retrieveRows();
          for (Row file : files) {
            filesTable.delete(file);
          }

          // Nullify job references (output_artifact_id, log_artifact_id)
          Table jobsTable = schema.getTable(HpcTables.JOBS);
          for (String refColumn : List.of(OUTPUT_ARTIFACT_ID, LOG_ARTIFACT_ID)) {
            List<Row> jobs = jobsTable.where(f(refColumn, EQUALS, artifactId)).retrieveRows();
            for (Row job : jobs) {
              job.set(refColumn, (String) null);
              jobsTable.update(job);
            }
          }

          artifactsTable.delete(artifact);
          logger.info("Artifact deleted: id={} files={}", artifactId, files.size());
          return artifact;
        });
  }

  /** Gets an artifact by ID. */
  public Row getArtifact(String artifactId) {
    return tx.txResult(
        db -> {
          List<Row> rows =
              db.getSchema(systemSchemaName)
                  .getTable(HpcTables.ARTIFACTS)
                  .where(f(ID, EQUALS, artifactId))
                  .retrieveRows();
          return rows.isEmpty() ? null : rows.getFirst();
        });
  }

  /** Lists files belonging to an artifact with optional prefix filter and pagination. */
  public List<Row> listFiles(String artifactId, String prefix, int limit, int offset) {
    return tx.txResult(
        db -> {
          Table table = db.getSchema(systemSchemaName).getTable(HpcTables.ARTIFACT_FILES);
          Query query = applyFileFilters(table.query(), artifactId, prefix);
          return query.orderBy(fileListOrder()).limit(limit).offset(offset).retrieveRows();
        });
  }

  /** Counts files belonging to an artifact with optional prefix filter. */
  public int countFiles(String artifactId, String prefix) {
    return tx.txResult(
        db -> {
          DSLContext jooq = ((SqlDatabase) db).getJooq();
          return jooq.fetchCount(
              jooq.selectFrom(table(name(systemSchemaName, HpcTables.ARTIFACT_FILES)))
                  .where(fileListCondition(artifactId, prefix)));
        });
  }

  private Query applyFileFilters(Query query, String artifactId, String prefix) {
    query.where(f(ARTIFACT_ID, EQUALS, artifactId));
    if (prefix != null && !prefix.isBlank()) {
      query.where(f(PATH, LIKE, prefix + "%"));
    }
    return query;
  }

  private Condition fileListCondition(String artifactId, String prefix) {
    Condition condition = field(name(ARTIFACT_ID)).eq(artifactId);
    if (prefix != null && !prefix.isBlank()) {
      condition = condition.and(field(name(PATH)).like(prefix + "%"));
    }
    return condition;
  }

  private Map<String, Order> fileListOrder() {
    Map<String, Order> order = new LinkedHashMap<>();
    order.put(PATH, Order.ASC);
    order.put(ID, Order.ASC);
    return order;
  }

  /**
   * Gets a file with its binary content by artifact ID and path. Uses explicit FILE column
   * selection to load binary data.
   */
  public Row getFileWithContent(String artifactId, String path) {
    return tx.txResult(
        db -> {
          Table table = db.getSchema(systemSchemaName).getTable(HpcTables.ARTIFACT_FILES);
          List<Row> rows =
              table
                  .query()
                  .select(
                      s(ID),
                      s(ARTIFACT_ID),
                      s(PATH),
                      s(SHA256),
                      s(SIZE_BYTES),
                      s(CONTENT_TYPE),
                      FILE_CONTENT_SELECT)
                  .where(f(ARTIFACT_ID, EQUALS, artifactId))
                  .where(f(PATH, EQUALS, path))
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
                  .getTable(HpcTables.ARTIFACT_FILES)
                  .where(f(ARTIFACT_ID, EQUALS, artifactId))
                  .where(f(PATH, EQUALS, path))
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
    // Defense-in-depth: validate path even if API layer already did
    InputValidator.validateFilePath(path, "path");
    return tx.txResult(
        db -> {
          Schema schema = db.getSchema(systemSchemaName);

          // Transition artifact to UPLOADING if currently CREATED
          Table artifactsTable = schema.getTable(HpcTables.ARTIFACTS);
          List<Row> artifacts = artifactsTable.where(f(ID, EQUALS, artifactId)).retrieveRows();
          if (artifacts.isEmpty()) {
            throw new MolgenisException("Artifact " + artifactId + " not found");
          }
          Row artifact = artifacts.getFirst();
          String currentStatus = artifact.getString(STATUS);
          if (ArtifactStatus.CREATED.name().equals(currentStatus)) {
            artifact.set(STATUS, ArtifactStatus.UPLOADING.name());
            artifactsTable.update(artifact);
          } else if (!ArtifactStatus.REGISTERED.name().equals(currentStatus)
              && !ArtifactStatus.UPLOADING.name().equals(currentStatus)) {
            throw new MolgenisException("Cannot add files to artifact in status " + currentStatus);
          }

          // Check if file already exists at this path
          Table filesTable = schema.getTable(HpcTables.ARTIFACT_FILES);
          List<Row> existing =
              filesTable
                  .where(f(ARTIFACT_ID, EQUALS, artifactId))
                  .where(f(PATH, EQUALS, path))
                  .retrieveRows();

          if (!existing.isEmpty()) {
            // Update existing file
            Row fileRow = existing.getFirst();
            fileRow.set(SHA256, sha256);
            fileRow.set(SIZE_BYTES, sizeBytes);
            fileRow.set(CONTENT_TYPE, contentType);
            if (content != null) {
              fileRow.set("content", content);
            }
            filesTable.update(fileRow);
            return fileRow.getString(ID);
          } else {
            // Insert new file
            String newId = UUID.randomUUID().toString();
            Row fileRow =
                row(
                    ID, newId,
                    ARTIFACT_ID, artifactId,
                    PATH, path,
                    SHA256, sha256,
                    SIZE_BYTES, sizeBytes,
                    CONTENT_TYPE, contentType);
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
          Table artifactsTable = schema.getTable(HpcTables.ARTIFACTS);
          List<Row> artifacts = artifactsTable.where(f(ID, EQUALS, artifactId)).retrieveRows();
          if (artifacts.isEmpty()) {
            return false;
          }
          String status = artifacts.getFirst().getString(STATUS);
          if (ArtifactStatus.COMMITTED.name().equals(status)) {
            throw new MolgenisException("Cannot delete files from committed artifact");
          }

          // Find and delete the file
          Table filesTable = schema.getTable(HpcTables.ARTIFACT_FILES);
          List<Row> files =
              filesTable
                  .where(f(ARTIFACT_ID, EQUALS, artifactId))
                  .where(f(PATH, EQUALS, path))
                  .retrieveRows();
          if (!files.isEmpty()) {
            filesTable.delete(files.getFirst());
            return true;
          }
          return false;
        });
  }
}
