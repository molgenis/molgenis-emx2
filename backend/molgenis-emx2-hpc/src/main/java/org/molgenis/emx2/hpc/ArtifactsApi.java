package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcApiUtils.requestId;
import static org.molgenis.emx2.hpc.HpcApiUtils.sanitizeDownloadFileName;
import static org.molgenis.emx2.hpc.HpcApiUtils.verifyContentSha256;
import static org.molgenis.emx2.hpc.HpcFields.*;
import static org.molgenis.emx2.hpc.protocol.InputValidator.parseIntParam;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.protocol.InputValidator;
import org.molgenis.emx2.hpc.protocol.LinkBuilder;
import org.molgenis.emx2.hpc.service.ArtifactService;
import org.molgenis.emx2.hpc.service.CommitResult;
import org.molgenis.emx2.sql.SqlDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Artifact CRUD endpoints:
 *
 * <ul>
 *   <li>POST /api/hpc/artifacts — create artifact
 *   <li>GET /api/hpc/artifacts/{id} — get artifact details
 *   <li>PUT /api/hpc/artifacts/{id}/files/{path} — upload file (binary or JSON metadata-only)
 *   <li>GET /api/hpc/artifacts/{id}/files — list files
 *   <li>POST /api/hpc/artifacts/{id}/commit — commit artifact with SHA-256 verification
 * </ul>
 */
public class ArtifactsApi {

  private static final Logger logger = LoggerFactory.getLogger(ArtifactsApi.class);

  /** Database setting key for the maximum upload size in bytes. */
  static final String MAX_UPLOAD_BYTES_SETTING = "MOLGENIS_HPC_MAX_UPLOAD_BYTES";

  /** Default maximum upload size: 500 MB. */
  static final long DEFAULT_MAX_UPLOAD_BYTES = 524_288_000L;

  /** Buffer size for streaming uploads to temp files. */
  private static final int STREAM_BUFFER_SIZE = 65_536;

  private static final String ARTIFACT_PREFIX = "Artifact ";
  private static final String NOT_FOUND_SUFFIX = " not found";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String OCTET_STREAM = "application/octet-stream";
  private static final String CONTENT_LENGTH_HEADER = "Content-Length";
  private static final String FILE_PREFIX = "File ";

  private final ArtifactService artifactService;
  private final SqlDatabase database;
  private final ArtifactResponseMapper mapper;
  private final Path secureTempDir;

  public ArtifactsApi(ArtifactService artifactService, SqlDatabase database) {
    this.artifactService = artifactService;
    this.database = database;
    this.mapper = new ArtifactResponseMapper();
    try {
      this.secureTempDir =
          Files.createTempDirectory(
              "hpc_uploads_",
              PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
      this.secureTempDir.toFile().deleteOnExit();
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create secure temp directory", e);
    }
  }

  /** POST /api/hpc/artifacts — create a new artifact. */
  @SuppressWarnings("unchecked")
  public void createArtifact(Context ctx) throws Exception {
    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String name = (String) body.get(NAME);
    String type = (String) body.get(TYPE);
    String residence = (String) body.get(RESIDENCE);
    String contentUrl = (String) body.get(CONTENT_URL);
    Object metadata = body.get(METADATA) != null ? MAPPER.valueToTree(body.get(METADATA)) : null;

    InputValidator.validateContentUrl(contentUrl, residence);

    String artifactId = artifactService.createArtifact(name, type, residence, contentUrl, metadata);

    boolean isExternal = residence != null && !"managed".equals(residence);
    ArtifactStatus status = isExternal ? ArtifactStatus.REGISTERED : ArtifactStatus.CREATED;

    Map<String, Object> response = new LinkedHashMap<>();
    response.put(ID, artifactId);
    response.put(NAME, name);
    response.put(TYPE, type);
    response.put(STATUS, status.name());
    response.put(LINKS, LinkBuilder.forArtifact(artifactId, status));

    ctx.status(201);
    ctx.json(response);
  }

  /** DELETE /api/hpc/artifacts/{id} — delete an artifact and its files. */
  public void deleteArtifact(Context ctx) {
    String artifactId = ctx.pathParam(ID);
    InputValidator.requireUuid(artifactId, ID);

    Row deleted = artifactService.deleteArtifact(artifactId);
    if (deleted == null) {
      throw HpcException.notFound(ARTIFACT_PREFIX + artifactId + NOT_FOUND_SUFFIX, requestId(ctx));
    }
    ctx.status(204);
  }

  /** GET /api/hpc/artifacts/{id} — get artifact with HATEOAS links. */
  public void getArtifact(Context ctx) {
    String artifactId = ctx.pathParam(ID);
    InputValidator.requireUuid(artifactId, ID);

    Row artifact = artifactService.getArtifact(artifactId);
    if (artifact == null) {
      throw HpcException.notFound(ARTIFACT_PREFIX + artifactId + NOT_FOUND_SUFFIX, requestId(ctx));
    }
    ctx.json(mapper.artifactToResponse(artifact));
  }

  /** GET /api/hpc/artifacts/{id}/files — list files in an artifact with pagination. */
  public void listFiles(Context ctx) {
    String artifactId = ctx.pathParam(ID);
    InputValidator.requireUuid(artifactId, ID);

    String prefix = ctx.queryParam("prefix");
    int limit = parseIntParam(ctx.queryParam("limit"), 100);
    int offset = parseIntParam(ctx.queryParam("offset"), 0);

    List<Row> files = artifactService.listFiles(artifactId, prefix, limit, offset);
    int totalCount = artifactService.countFiles(artifactId, prefix);

    List<Map<String, Object>> items =
        files.stream()
            .map(
                f -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put(ID, f.getString(ID));
                  m.put(PATH, f.getString(PATH));
                  m.put(SHA256, f.getString(SHA256));
                  m.put(SIZE_BYTES, f.getString(SIZE_BYTES));
                  m.put(CONTENT_TYPE, f.getString(CONTENT_TYPE));
                  m.put(
                      LINKS,
                      Map.of(
                          "content",
                          Map.of(
                              "href",
                              "/api/hpc/artifacts/" + artifactId + "/files/" + f.getString(PATH),
                              "method",
                              "GET")));
                  return m;
                })
            .toList();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("items", items);
    response.put("count", items.size());
    response.put("total_count", totalCount);
    response.put("limit", limit);
    response.put("offset", offset);
    ctx.header("X-Total-Count", String.valueOf(totalCount));
    ctx.json(response);
  }

  /** Holder for the result of an upload operation. */
  private record UploadResult(
      String sha256, long sizeBytes, String contentType, BinaryFileWrapper content) {}

  /**
   * PUT /api/hpc/artifacts/{id}/files/{path} — upload file by path.
   *
   * <p>Supports two modes:
   *
   * <ul>
   *   <li>Binary upload: raw bytes or multipart with "file" part — content is stored and SHA-256 is
   *       computed from the bytes. Binary uploads are streamed to a temp file to avoid holding the
   *       entire payload in JVM heap.
   *   <li>JSON metadata-only: Content-Type application/json with {"sha256", "size_bytes",
   *       "content_type"} — registers file metadata without storing binary content (used for posix
   *       artifacts).
   * </ul>
   *
   * <p>Upload size is bounded by the {@code MOLGENIS_HPC_MAX_UPLOAD_BYTES} database setting
   * (default 500 MB). The Content-Length header is checked eagerly, and the byte count is also
   * enforced during streaming to guard against chunked-transfer or mismatched headers.
   */
  @SuppressWarnings("unchecked")
  public void uploadFileByPath(Context ctx) {
    String artifactId = ctx.pathParam(ID);
    String filePath = ctx.pathParam(PATH);
    InputValidator.requireUuid(artifactId, ID);
    InputValidator.validateFilePath(filePath, PATH);

    File tempFile = null;
    try {
      String ct = ctx.header(CONTENT_TYPE_HEADER);
      boolean isMultipart = ct != null && ct.startsWith("multipart/form-data");
      boolean isJson = ct != null && ct.startsWith("application/json");

      UploadResult result;
      if (isJson) {
        result = handleJsonMetadataUpload(ctx);
      } else if (isMultipart) {
        tempFile = createSecureTempFile("hpc_upload_", ".tmp");
        result = handleMultipartUpload(ctx, filePath, tempFile);
      } else {
        tempFile = createSecureTempFile("hpc_upload_", ".tmp");
        result = handleBinaryUpload(ctx, filePath, ct, tempFile);
      }

      String fileId =
          artifactService.uploadFileByPath(
              artifactId,
              filePath,
              result.sha256(),
              result.sizeBytes(),
              result.contentType(),
              result.content());

      Map<String, Object> response = new LinkedHashMap<>();
      response.put(ID, fileId);
      response.put(ARTIFACT_ID, artifactId);
      response.put(PATH, filePath);
      response.put(SHA256, result.sha256());
      response.put(SIZE_BYTES, result.sizeBytes());

      ctx.status(201);
      ctx.json(response);
    } catch (HpcException e) {
      throw e;
    } catch (MolgenisException e) {
      if (e.getMessage() != null && e.getMessage().contains("not found")) {
        throw HpcException.notFound(e.getMessage(), requestId(ctx));
      }
      throw HpcException.internal(e.getMessage(), requestId(ctx));
    } catch (Exception e) {
      throw HpcException.internal(e.getMessage(), requestId(ctx));
    } finally {
      if (tempFile != null) {
        if (!tempFile.delete() && tempFile.exists()) {
          logger.warn("Failed to delete temp file: {}", tempFile.getAbsolutePath());
        }
      }
    }
  }

  /** Creates a temp file inside the secure private temp directory. */
  private File createSecureTempFile(String prefix, String suffix) throws IOException {
    return Files.createTempFile(secureTempDir, prefix, suffix).toFile();
  }

  /** Handles JSON metadata-only upload (no binary content). */
  @SuppressWarnings("unchecked")
  private UploadResult handleJsonMetadataUpload(Context ctx) throws IOException {
    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String sha256 = (String) body.get(SHA256);
    long sizeBytes = body.get(SIZE_BYTES) != null ? ((Number) body.get(SIZE_BYTES)).longValue() : 0;
    String contentType = (String) body.get(CONTENT_TYPE);
    if (contentType == null || contentType.isBlank()) {
      contentType = OCTET_STREAM;
    }
    return new UploadResult(sha256, sizeBytes, contentType, null);
  }

  /** Handles multipart file upload, streaming to a temp file with SHA-256 computation. */
  private UploadResult handleMultipartUpload(Context ctx, String filePath, File tempFile)
      throws Exception {
    long maxBytes = getMaxUploadBytes();
    enforceContentLengthLimit(ctx, maxBytes);

    ctx.attribute(
        "org.eclipse.jetty.multipartConfig",
        new MultipartConfigElement(tempFile.getAbsolutePath()));
    Part filePart = ctx.req().getPart("file");
    if (filePart == null) {
      throw HpcException.badRequest("Multipart 'file' part is required", requestId(ctx));
    }

    File multipartTemp = createSecureTempFile("hpc_mp_upload_", ".tmp");
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    long sizeBytes;
    try (InputStream input = filePart.getInputStream()) {
      sizeBytes = streamToFile(input, multipartTemp, digest, maxBytes, requestId(ctx));
    } catch (HpcException e) {
      multipartTemp.delete();
      throw e;
    }
    // Replace tempFile reference so both get cleaned up
    tempFile.delete();

    String sha256 = HexFormat.of().formatHex(digest.digest());
    String contentType = ctx.formParam(CONTENT_TYPE);
    if (contentType == null) {
      contentType = filePart.getContentType();
    }
    contentType = resolveContentType(filePath, contentType);

    verifyContentSha256(ctx, sha256);
    BinaryFileWrapper content =
        new BinaryFileWrapper(contentType, filePath, Files.readAllBytes(multipartTemp.toPath()));
    multipartTemp.delete();

    return new UploadResult(sha256, sizeBytes, contentType, content);
  }

  /** Handles raw binary upload, streaming to a temp file with SHA-256 computation. */
  private UploadResult handleBinaryUpload(
      Context ctx, String filePath, String declaredType, File tempFile) throws Exception {
    long maxBytes = getMaxUploadBytes();
    enforceContentLengthLimit(ctx, maxBytes);

    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    long sizeBytes;
    try (InputStream input = ctx.req().getInputStream()) {
      sizeBytes = streamToFile(input, tempFile, digest, maxBytes, requestId(ctx));
    } catch (HpcException e) {
      throw e;
    }

    String sha256 = HexFormat.of().formatHex(digest.digest());
    String contentType = resolveContentType(filePath, declaredType);

    verifyContentSha256(ctx, sha256);
    BinaryFileWrapper content =
        new BinaryFileWrapper(contentType, filePath, Files.readAllBytes(tempFile.toPath()));

    return new UploadResult(sha256, sizeBytes, contentType, content);
  }

  /**
   * Resolves the content type for a file. Guesses from the file name first; falls back to the
   * declared type or {@code application/octet-stream}.
   */
  private static String resolveContentType(String filePath, String declaredType) {
    String guessed = URLConnection.guessContentTypeFromName(filePath);
    if (guessed != null) {
      return guessed;
    }
    if (declaredType != null && !declaredType.isBlank()) {
      return declaredType;
    }
    return OCTET_STREAM;
  }

  /**
   * Reads the {@code MOLGENIS_HPC_MAX_UPLOAD_BYTES} database setting. Falls back to {@link
   * #DEFAULT_MAX_UPLOAD_BYTES} when the setting is absent or unparseable.
   */
  long getMaxUploadBytes() {
    String value = HpcAuth.readSetting(database, MAX_UPLOAD_BYTES_SETTING);
    if (value != null && !value.isBlank()) {
      try {
        long parsed = Long.parseLong(value.trim());
        if (parsed > 0) {
          return parsed;
        }
      } catch (NumberFormatException ignored) {
        logger.warn(
            "Invalid {} setting '{}', using default {}",
            MAX_UPLOAD_BYTES_SETTING,
            value,
            DEFAULT_MAX_UPLOAD_BYTES);
      }
    }
    return DEFAULT_MAX_UPLOAD_BYTES;
  }

  /**
   * Checks the Content-Length header against the maximum upload size. Returns 413 Payload Too Large
   * before reading the body when the declared size exceeds the limit.
   */
  private static void enforceContentLengthLimit(Context ctx, long maxBytes) {
    String contentLengthHeader = ctx.header(CONTENT_LENGTH_HEADER);
    if (contentLengthHeader != null) {
      try {
        long declaredSize = Long.parseLong(contentLengthHeader.trim());
        if (declaredSize > maxBytes) {
          throw HpcException.payloadTooLarge(
              "Content-Length "
                  + declaredSize
                  + " exceeds maximum upload size of "
                  + maxBytes
                  + " bytes",
              requestId(ctx));
        }
      } catch (NumberFormatException ignored) {
        // Unparseable Content-Length — let streaming enforcement handle it
      }
    }
  }

  /**
   * Streams an InputStream to a file while computing SHA-256 incrementally and enforcing a maximum
   * byte count. Returns the total number of bytes written.
   *
   * @throws HpcException with 413 status if the stream exceeds maxBytes
   * @throws IOException on I/O errors
   */
  private static long streamToFile(
      InputStream input, File dest, MessageDigest digest, long maxBytes, String requestId)
      throws IOException {
    long totalBytes = 0;
    byte[] buffer = new byte[STREAM_BUFFER_SIZE];
    try (OutputStream out = new FileOutputStream(dest)) {
      int bytesRead;
      while ((bytesRead = input.read(buffer)) != -1) {
        totalBytes += bytesRead;
        if (totalBytes > maxBytes) {
          throw HpcException.payloadTooLarge(
              "Upload exceeds maximum size of " + maxBytes + " bytes", requestId);
        }
        digest.update(buffer, 0, bytesRead);
        out.write(buffer, 0, bytesRead);
      }
    }
    return totalBytes;
  }

  /** GET /api/hpc/artifacts/{id}/files/{path} — download file content. */
  public void downloadFile(Context ctx) {
    String artifactId = ctx.pathParam(ID);
    String filePath = ctx.pathParam(PATH);
    InputValidator.requireUuid(artifactId, ID);

    Row file = artifactService.getFileWithContent(artifactId, filePath);
    if (file == null) {
      // Check if parent artifact has a content_url for redirect
      Row artifact = artifactService.getArtifact(artifactId);
      if (artifact != null && artifact.getString(CONTENT_URL) != null) {
        ctx.redirect(artifact.getString(CONTENT_URL) + "/" + filePath);
        return;
      }
      throw HpcException.notFound(
          FILE_PREFIX + filePath + " not found in artifact " + artifactId, requestId(ctx));
    }

    byte[] bytes = file.getBinary("content_contents");
    if (bytes == null) {
      // Metadata-only file — check parent artifact for content_url
      Row artifact = artifactService.getArtifact(artifactId);
      if (artifact != null && artifact.getString(CONTENT_URL) != null) {
        ctx.redirect(artifact.getString(CONTENT_URL) + "/" + filePath);
        return;
      }
      throw HpcException.notFound(FILE_PREFIX + filePath + " has no content", requestId(ctx));
    }

    String contentType = file.getString("content_mimetype");
    if (contentType == null) {
      contentType = file.getString(CONTENT_TYPE);
    }
    if (contentType == null) {
      contentType = OCTET_STREAM;
    }

    String downloadFileName = sanitizeDownloadFileName(filePath);
    ctx.header(CONTENT_TYPE_HEADER, contentType);
    ctx.header("Content-Disposition", HpcApiUtils.buildContentDispositionHeader(downloadFileName));
    if (file.getString(SHA256) != null) {
      ctx.header("X-Content-SHA256", file.getString(SHA256));
    }
    ctx.header(CONTENT_LENGTH_HEADER, String.valueOf(bytes.length));
    ctx.result(bytes);
  }

  /** HEAD /api/hpc/artifacts/{id}/files/{path} — file metadata in headers. */
  public void headFile(Context ctx) {
    String artifactId = ctx.pathParam(ID);
    String filePath = ctx.pathParam(PATH);
    InputValidator.requireUuid(artifactId, ID);

    Row file = artifactService.getFileMetadata(artifactId, filePath);
    if (file == null) {
      ctx.status(404);
      return;
    }
    if (file.getString(SHA256) != null) {
      ctx.header("X-Content-SHA256", file.getString(SHA256));
    }
    if (file.getString(SIZE_BYTES) != null) {
      ctx.header(CONTENT_LENGTH_HEADER, file.getString(SIZE_BYTES));
    }
    if (file.getString(CONTENT_TYPE) != null) {
      ctx.header(CONTENT_TYPE_HEADER, file.getString(CONTENT_TYPE));
    }
    ctx.status(200);
  }

  /** DELETE /api/hpc/artifacts/{id}/files/{path} — delete file before commit. */
  public void deleteFile(Context ctx) {
    String artifactId = ctx.pathParam(ID);
    String filePath = ctx.pathParam(PATH);
    InputValidator.requireUuid(artifactId, ID);

    try {
      boolean deleted = artifactService.deleteFile(artifactId, filePath);
      if (!deleted) {
        throw HpcException.notFound(
            FILE_PREFIX + filePath + " not found in artifact " + artifactId, requestId(ctx));
      }
      ctx.status(204);
    } catch (HpcException e) {
      throw e;
    } catch (MolgenisException e) {
      if (e.getMessage() != null && e.getMessage().contains("committed")) {
        throw HpcException.conflict(e.getMessage(), requestId(ctx));
      }
      throw HpcException.internal(e.getMessage(), requestId(ctx));
    }
  }

  /**
   * POST /api/hpc/artifacts/{id}/commit — commit with SHA-256 verification.
   *
   * <p>Request body: {"sha256": "overall-hash...", "size_bytes": 4096}
   */
  @SuppressWarnings("unchecked")
  public void commitArtifact(Context ctx) throws Exception {
    String artifactId = ctx.pathParam(ID);
    InputValidator.requireUuid(artifactId, ID);

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String sha256 = (String) body.get(SHA256);
    Long sizeBytes =
        body.get(SIZE_BYTES) != null ? ((Number) body.get(SIZE_BYTES)).longValue() : null;

    CommitResult commitResult = artifactService.commitArtifact(artifactId, sha256, sizeBytes);
    if (commitResult == null) {
      throw HpcException.notFound(ARTIFACT_PREFIX + artifactId + NOT_FOUND_SUFFIX, requestId(ctx));
    }
    if (!commitResult.isSuccess()) {
      String title = commitResult.isHashMismatch() ? "Hash Mismatch" : "Conflict";
      throw new HpcException(409, title, commitResult.error(), requestId(ctx));
    }

    ctx.status(200);
    ctx.json(mapper.artifactToResponse(commitResult.artifact()));
  }
}
