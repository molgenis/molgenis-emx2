package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.HpcApiUtils.requestId;
import static org.molgenis.emx2.hpc.HpcApiUtils.sanitizeDownloadFileName;
import static org.molgenis.emx2.hpc.HpcApiUtils.verifyContentSha256;
import static org.molgenis.emx2.hpc.protocol.InputValidator.parseIntParam;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
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

  private final ArtifactService artifactService;
  private final ArtifactResponseMapper mapper;

  public ArtifactsApi(ArtifactService artifactService) {
    this.artifactService = artifactService;
    this.mapper = new ArtifactResponseMapper();
  }

  /** POST /api/hpc/artifacts — create a new artifact. */
  @SuppressWarnings("unchecked")
  public void createArtifact(Context ctx) throws Exception {
    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String name = (String) body.get("name");
    String type = (String) body.get("type");
    String residence = (String) body.get("residence");
    String contentUrl = (String) body.get("content_url");
    Object metadata =
        body.get("metadata") != null ? MAPPER.valueToTree(body.get("metadata")) : null;

    InputValidator.validateContentUrl(contentUrl, residence);

    String artifactId = artifactService.createArtifact(name, type, residence, contentUrl, metadata);

    boolean isExternal = residence != null && !"managed".equals(residence);
    ArtifactStatus status = isExternal ? ArtifactStatus.REGISTERED : ArtifactStatus.CREATED;

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", artifactId);
    response.put("name", name);
    response.put("type", type);
    response.put("status", status.name());
    response.put("_links", LinkBuilder.forArtifact(artifactId, status));

    ctx.status(201);
    ctx.json(response);
  }

  /** DELETE /api/hpc/artifacts/{id} — delete an artifact and its files. */
  public void deleteArtifact(Context ctx) {
    String artifactId = ctx.pathParam("id");
    InputValidator.requireUuid(artifactId, "id");

    Row deleted = artifactService.deleteArtifact(artifactId);
    if (deleted == null) {
      throw HpcException.notFound("Artifact " + artifactId + " not found", requestId(ctx));
    }
    ctx.status(204);
  }

  /** GET /api/hpc/artifacts/{id} — get artifact with HATEOAS links. */
  public void getArtifact(Context ctx) {
    String artifactId = ctx.pathParam("id");
    InputValidator.requireUuid(artifactId, "id");

    Row artifact = artifactService.getArtifact(artifactId);
    if (artifact == null) {
      throw HpcException.notFound("Artifact " + artifactId + " not found", requestId(ctx));
    }
    ctx.json(mapper.artifactToResponse(artifact));
  }

  /** GET /api/hpc/artifacts/{id}/files — list files in an artifact with pagination. */
  public void listFiles(Context ctx) {
    String artifactId = ctx.pathParam("id");
    InputValidator.requireUuid(artifactId, "id");

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
                  m.put("id", f.getString("id"));
                  m.put("path", f.getString("path"));
                  m.put("sha256", f.getString("sha256"));
                  m.put("size_bytes", f.getString("size_bytes"));
                  m.put("content_type", f.getString("content_type"));
                  m.put(
                      "_links",
                      Map.of(
                          "content",
                          Map.of(
                              "href",
                              "/api/hpc/artifacts/" + artifactId + "/files/" + f.getString("path"),
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

  /**
   * PUT /api/hpc/artifacts/{id}/files/{path} — upload file by path.
   *
   * <p>Supports two modes:
   *
   * <ul>
   *   <li>Binary upload: raw bytes or multipart with "file" part — content is stored and SHA-256 is
   *       computed from the bytes.
   *   <li>JSON metadata-only: Content-Type application/json with {"sha256", "size_bytes",
   *       "content_type"} — registers file metadata without storing binary content (used for posix
   *       artifacts).
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public void uploadFileByPath(Context ctx) {
    String artifactId = ctx.pathParam("id");
    String filePath = ctx.pathParam("path");
    InputValidator.requireUuid(artifactId, "id");
    InputValidator.validateFilePath(filePath, "path");

    File tempFile = null;
    try {
      String ct = ctx.header("Content-Type");
      boolean isMultipart = ct != null && ct.startsWith("multipart/form-data");
      boolean isJson = ct != null && ct.startsWith("application/json");

      String sha256;
      long sizeBytes;
      String contentType;
      BinaryFileWrapper content;

      if (isJson) {
        // JSON metadata-only mode (no binary upload)
        Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
        sha256 = (String) body.get("sha256");
        sizeBytes =
            body.get("size_bytes") != null ? ((Number) body.get("size_bytes")).longValue() : 0;
        contentType = (String) body.get("content_type");
        if (contentType == null || contentType.isBlank()) {
          contentType = "application/octet-stream";
        }
        content = null;
      } else if (isMultipart) {
        tempFile = File.createTempFile("hpc_upload_", ".tmp");
        ctx.attribute(
            "org.eclipse.jetty.multipartConfig",
            new MultipartConfigElement(tempFile.getAbsolutePath()));
        Part filePart = ctx.req().getPart("file");
        if (filePart == null) {
          throw HpcException.badRequest("Multipart 'file' part is required", requestId(ctx));
        }
        byte[] fileBytes;
        try (InputStream input = filePart.getInputStream()) {
          fileBytes = input.readAllBytes();
        }
        contentType = ctx.formParam("content_type");
        if (contentType == null) {
          contentType = filePart.getContentType();
        }

        String guessed = URLConnection.guessContentTypeFromName(filePath);
        if (guessed != null) {
          contentType = guessed;
        } else if (contentType == null || contentType.isBlank()) {
          contentType = "application/octet-stream";
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        sha256 = HexFormat.of().formatHex(digest.digest(fileBytes));
        sizeBytes = fileBytes.length;
        content = new BinaryFileWrapper(contentType, filePath, fileBytes);

        verifyContentSha256(ctx, sha256);
      } else {
        // Raw binary upload
        byte[] fileBytes = ctx.bodyAsBytes();
        contentType = ct;

        String guessed = URLConnection.guessContentTypeFromName(filePath);
        if (guessed != null) {
          contentType = guessed;
        } else if (contentType == null || contentType.isBlank()) {
          contentType = "application/octet-stream";
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        sha256 = HexFormat.of().formatHex(digest.digest(fileBytes));
        sizeBytes = fileBytes.length;
        content = new BinaryFileWrapper(contentType, filePath, fileBytes);

        verifyContentSha256(ctx, sha256);
      }

      String fileId =
          artifactService.uploadFileByPath(
              artifactId, filePath, sha256, sizeBytes, contentType, content);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("id", fileId);
      response.put("artifact_id", artifactId);
      response.put("path", filePath);
      response.put("sha256", sha256);
      response.put("size_bytes", sizeBytes);

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
        tempFile.delete();
      }
    }
  }

  /** GET /api/hpc/artifacts/{id}/files/{path} — download file content. */
  public void downloadFile(Context ctx) {
    String artifactId = ctx.pathParam("id");
    String filePath = ctx.pathParam("path");
    InputValidator.requireUuid(artifactId, "id");

    Row file = artifactService.getFileWithContent(artifactId, filePath);
    if (file == null) {
      // Check if parent artifact has a content_url for redirect
      Row artifact = artifactService.getArtifact(artifactId);
      if (artifact != null && artifact.getString("content_url") != null) {
        ctx.redirect(artifact.getString("content_url") + "/" + filePath);
        return;
      }
      throw HpcException.notFound(
          "File " + filePath + " not found in artifact " + artifactId, requestId(ctx));
    }

    byte[] bytes = file.getBinary("content_contents");
    if (bytes == null) {
      // Metadata-only file — check parent artifact for content_url
      Row artifact = artifactService.getArtifact(artifactId);
      if (artifact != null && artifact.getString("content_url") != null) {
        ctx.redirect(artifact.getString("content_url") + "/" + filePath);
        return;
      }
      throw HpcException.notFound("File " + filePath + " has no content", requestId(ctx));
    }

    String contentType = file.getString("content_mimetype");
    if (contentType == null) {
      contentType = file.getString("content_type");
    }
    if (contentType == null) {
      contentType = "application/octet-stream";
    }

    String downloadFileName = sanitizeDownloadFileName(filePath);
    ctx.header("Content-Type", contentType);
    ctx.header("Content-Disposition", HpcApiUtils.buildContentDispositionHeader(downloadFileName));
    if (file.getString("sha256") != null) {
      ctx.header("X-Content-SHA256", file.getString("sha256"));
    }
    ctx.header("Content-Length", String.valueOf(bytes.length));
    ctx.result(bytes);
  }

  /** HEAD /api/hpc/artifacts/{id}/files/{path} — file metadata in headers. */
  public void headFile(Context ctx) {
    String artifactId = ctx.pathParam("id");
    String filePath = ctx.pathParam("path");
    InputValidator.requireUuid(artifactId, "id");

    Row file = artifactService.getFileMetadata(artifactId, filePath);
    if (file == null) {
      ctx.status(404);
      return;
    }
    if (file.getString("sha256") != null) {
      ctx.header("X-Content-SHA256", file.getString("sha256"));
    }
    if (file.getString("size_bytes") != null) {
      ctx.header("Content-Length", file.getString("size_bytes"));
    }
    if (file.getString("content_type") != null) {
      ctx.header("Content-Type", file.getString("content_type"));
    }
    ctx.status(200);
  }

  /** DELETE /api/hpc/artifacts/{id}/files/{path} — delete file before commit. */
  public void deleteFile(Context ctx) {
    String artifactId = ctx.pathParam("id");
    String filePath = ctx.pathParam("path");
    InputValidator.requireUuid(artifactId, "id");

    try {
      boolean deleted = artifactService.deleteFile(artifactId, filePath);
      if (!deleted) {
        throw HpcException.notFound(
            "File " + filePath + " not found in artifact " + artifactId, requestId(ctx));
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
    String artifactId = ctx.pathParam("id");
    InputValidator.requireUuid(artifactId, "id");

    Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
    String sha256 = (String) body.get("sha256");
    Long sizeBytes =
        body.get("size_bytes") != null ? ((Number) body.get("size_bytes")).longValue() : null;

    CommitResult commitResult = artifactService.commitArtifact(artifactId, sha256, sizeBytes);
    if (commitResult == null) {
      throw HpcException.notFound("Artifact " + artifactId + " not found", requestId(ctx));
    }
    if (!commitResult.isSuccess()) {
      String title = commitResult.isHashMismatch() ? "Hash Mismatch" : "Conflict";
      throw new HpcException(409, title, commitResult.error(), requestId(ctx));
    }

    ctx.status(200);
    ctx.json(mapper.artifactToResponse(commitResult.artifact()));
  }
}
