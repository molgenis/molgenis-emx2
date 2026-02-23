package org.molgenis.emx2.hpc;

import static org.molgenis.emx2.hpc.protocol.InputValidator.parseIntParam;
import static org.molgenis.emx2.hpc.protocol.Json.MAPPER;

import io.javalin.http.Context;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.Part;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.BinaryFileWrapper;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.protocol.InputValidator;
import org.molgenis.emx2.hpc.protocol.LinkBuilder;
import org.molgenis.emx2.hpc.protocol.ProblemDetail;
import org.molgenis.emx2.hpc.service.ArtifactService;
import org.molgenis.emx2.hpc.service.CommitResult;

/**
 * Artifact CRUD endpoints:
 *
 * <ul>
 *   <li>POST /api/hpc/artifacts — create artifact
 *   <li>GET /api/hpc/artifacts/{id} — get artifact details
 *   <li>POST /api/hpc/artifacts/{id}/files — upload a file to an artifact
 *   <li>GET /api/hpc/artifacts/{id}/files — list files
 *   <li>POST /api/hpc/artifacts/{id}/commit — commit artifact with SHA-256 verification
 * </ul>
 */
public class ArtifactsApi {

  private final ArtifactService artifactService;

  public ArtifactsApi(ArtifactService artifactService) {
    this.artifactService = artifactService;
  }

  /** POST /api/hpc/artifacts — create a new artifact. */
  @SuppressWarnings("unchecked")
  public void createArtifact(Context ctx) {
    try {
      Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
      String name = (String) body.get("name");
      String type = (String) body.get("type");
      String residence = (String) body.get("residence");
      String contentUrl = (String) body.get("content_url");
      String metadata =
          body.get("metadata") != null ? MAPPER.writeValueAsString(body.get("metadata")) : null;

      String artifactId =
          artifactService.createArtifact(name, type, residence, contentUrl, metadata);

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
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  /** GET /api/hpc/artifacts/{id} — get artifact with HATEOAS links. */
  public void getArtifact(Context ctx) {
    String artifactId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    Row artifact = artifactService.getArtifact(artifactId);
    if (artifact == null) {
      ProblemDetail.send(
          ctx,
          404,
          "Not Found",
          "Artifact " + artifactId + " not found",
          ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    ctx.json(artifactToResponse(artifact));
  }

  /**
   * POST /api/hpc/artifacts/{id}/files — upload a file.
   *
   * <p>Supports two modes:
   *
   * <ul>
   *   <li>Multipart: Content-Type multipart/form-data with "file" part and optional form params
   *       (path, role, sha256, content_type)
   *   <li>JSON metadata-only: Content-Type application/json with {"path", "role", "sha256",
   *       "size_bytes", "content_type"}
   * </ul>
   */
  @SuppressWarnings("unchecked")
  public void uploadFile(Context ctx) {
    String artifactId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    File tempFile = null;
    try {
      String ct = ctx.header("Content-Type");
      boolean isMultipart = ct != null && ct.startsWith("multipart/form-data");

      String path;
      String sha256;
      Long sizeBytes;
      String contentType;
      BinaryFileWrapper content = null;

      if (isMultipart) {
        // Configure multipart handling
        tempFile = File.createTempFile("hpc_upload_", ".tmp");
        ctx.attribute(
            "org.eclipse.jetty.multipartConfig",
            new MultipartConfigElement(tempFile.getAbsolutePath()));

        Part filePart = ctx.req().getPart("file");
        if (filePart == null) {
          ProblemDetail.send(
              ctx,
              400,
              "Bad Request",
              "Multipart 'file' part is required",
              ctx.header(HpcHeaders.REQUEST_ID));
          return;
        }

        // Read file content and compute SHA-256
        byte[] fileBytes;
        try (InputStream input = filePart.getInputStream()) {
          fileBytes = input.readAllBytes();
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String computedSha256 = HexFormat.of().formatHex(digest.digest(fileBytes));

        // Extract metadata from form params, fall back to part metadata
        path = ctx.formParam("path");
        if (path == null || path.isBlank()) {
          path = filePart.getSubmittedFileName();
        }
        sha256 = ctx.formParam("sha256");
        if (sha256 == null) {
          sha256 = computedSha256;
        }
        sizeBytes = (long) fileBytes.length;
        contentType = ctx.formParam("content_type");
        if (contentType == null) {
          contentType = filePart.getContentType();
        }

        content =
            new BinaryFileWrapper(
                contentType != null ? contentType : "application/octet-stream", path, fileBytes);
      } else {
        // JSON metadata-only mode
        Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
        path = (String) body.get("path");
        sha256 = (String) body.get("sha256");
        sizeBytes =
            body.get("size_bytes") != null ? ((Number) body.get("size_bytes")).longValue() : null;
        contentType = (String) body.get("content_type");
      }

      if (path == null || path.isBlank()) {
        ProblemDetail.send(
            ctx, 400, "Bad Request", "path is required", ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      String fileId =
          artifactService.uploadFile(artifactId, path, sha256, sizeBytes, contentType, content);

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("id", fileId);
      response.put("artifact_id", artifactId);
      response.put("path", path);
      response.put("sha256", sha256);
      response.put("size_bytes", sizeBytes);

      ctx.status(201);
      ctx.json(response);
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    } finally {
      if (tempFile != null) {
        tempFile.delete();
      }
    }
  }

  /** GET /api/hpc/artifacts/{id}/files — list files in an artifact with pagination. */
  public void listFiles(Context ctx) {
    String artifactId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
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

  /** PUT /api/hpc/artifacts/{id}/files/{path} — upload file by path. */
  public void uploadFileByPath(Context ctx) {
    String artifactId = ctx.pathParam("id");
    String filePath = ctx.pathParam("path");
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    if (filePath == null || filePath.isBlank()) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", "file path is required", ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    File tempFile = null;
    try {
      String ct = ctx.header("Content-Type");
      boolean isMultipart = ct != null && ct.startsWith("multipart/form-data");

      byte[] fileBytes;
      String contentType;

      if (isMultipart) {
        tempFile = File.createTempFile("hpc_upload_", ".tmp");
        ctx.attribute(
            "org.eclipse.jetty.multipartConfig",
            new MultipartConfigElement(tempFile.getAbsolutePath()));
        Part filePart = ctx.req().getPart("file");
        if (filePart == null) {
          ProblemDetail.send(
              ctx,
              400,
              "Bad Request",
              "Multipart 'file' part is required",
              ctx.header(HpcHeaders.REQUEST_ID));
          return;
        }
        try (InputStream input = filePart.getInputStream()) {
          fileBytes = input.readAllBytes();
        }
        contentType = ctx.formParam("content_type");
        if (contentType == null) {
          contentType = filePart.getContentType();
        }
      } else {
        fileBytes = ctx.bodyAsBytes();
        contentType = ct;
      }

      if (contentType == null || contentType.isBlank()) {
        contentType = "application/octet-stream";
      }

      // Compute SHA-256
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      String sha256 = HexFormat.of().formatHex(digest.digest(fileBytes));
      long sizeBytes = fileBytes.length;

      BinaryFileWrapper content = new BinaryFileWrapper(contentType, filePath, fileBytes);

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
    } catch (MolgenisException e) {
      if (e.getMessage() != null && e.getMessage().contains("not found")) {
        ProblemDetail.send(
            ctx, 404, "Not Found", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      } else {
        ProblemDetail.send(
            ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      }
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
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
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    try {
      Row file = artifactService.getFileWithContent(artifactId, filePath);
      if (file == null) {
        // Check if parent artifact has a content_url for redirect
        Row artifact = artifactService.getArtifact(artifactId);
        if (artifact != null && artifact.getString("content_url") != null) {
          ctx.redirect(artifact.getString("content_url") + "/" + filePath);
          return;
        }
        ProblemDetail.send(
            ctx,
            404,
            "Not Found",
            "File " + filePath + " not found in artifact " + artifactId,
            ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      byte[] bytes = file.getBinary("content_contents");
      if (bytes == null) {
        // Metadata-only file — check parent artifact for content_url
        Row artifact = artifactService.getArtifact(artifactId);
        if (artifact != null && artifact.getString("content_url") != null) {
          ctx.redirect(artifact.getString("content_url") + "/" + filePath);
          return;
        }
        ProblemDetail.send(
            ctx,
            404,
            "Not Found",
            "File " + filePath + " has no content",
            ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      String contentType = file.getString("content_mimetype");
      if (contentType == null) {
        contentType = file.getString("content_type");
      }
      if (contentType == null) {
        contentType = "application/octet-stream";
      }

      ctx.header("Content-Type", contentType);
      ctx.header("Content-Disposition", "attachment; filename=\"" + filePath + "\"");
      if (file.getString("sha256") != null) {
        ctx.header("X-Content-SHA256", file.getString("sha256"));
      }
      ctx.header("Content-Length", String.valueOf(bytes.length));
      ctx.result(bytes);
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  /** HEAD /api/hpc/artifacts/{id}/files/{path} — file metadata in headers. */
  public void headFile(Context ctx) {
    String artifactId = ctx.pathParam("id");
    String filePath = ctx.pathParam("path");
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
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
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    try {
      boolean deleted = artifactService.deleteFile(artifactId, filePath);
      if (!deleted) {
        ProblemDetail.send(
            ctx,
            404,
            "Not Found",
            "File " + filePath + " not found in artifact " + artifactId,
            ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }
      ctx.status(204);
    } catch (MolgenisException e) {
      if (e.getMessage() != null && e.getMessage().contains("committed")) {
        ProblemDetail.send(ctx, 409, "Conflict", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      } else {
        ProblemDetail.send(
            ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      }
    }
  }

  /**
   * POST /api/hpc/artifacts/{id}/commit — commit with SHA-256 verification.
   *
   * <p>Request body: {"sha256": "overall-hash...", "size_bytes": 4096}
   */
  @SuppressWarnings("unchecked")
  public void commitArtifact(Context ctx) {
    String artifactId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    try {
      Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
      String sha256 = (String) body.get("sha256");
      Long sizeBytes =
          body.get("size_bytes") != null ? ((Number) body.get("size_bytes")).longValue() : null;

      CommitResult commitResult = artifactService.commitArtifact(artifactId, sha256, sizeBytes);
      if (commitResult == null) {
        // Artifact not found
        ProblemDetail.send(
            ctx,
            404,
            "Not Found",
            "Artifact " + artifactId + " not found",
            ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }
      if (!commitResult.isSuccess()) {
        int status = commitResult.isHashMismatch() ? 409 : 409;
        String title = commitResult.isHashMismatch() ? "Hash Mismatch" : "Conflict";
        ProblemDetail.send(
            ctx, status, title, commitResult.error(), ctx.header(HpcHeaders.REQUEST_ID));
        return;
      }

      ctx.status(200);
      ctx.json(artifactToResponse(commitResult.artifact()));
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  private Map<String, Object> artifactToResponse(Row artifact) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", artifact.getString("id"));
    response.put("name", artifact.getString("name"));
    response.put("type", artifact.getString("type"));
    response.put("residence", artifact.getString("residence"));
    response.put("status", artifact.getString("status"));
    response.put("sha256", artifact.getString("sha256"));
    response.put("size_bytes", artifact.getString("size_bytes"));
    response.put("content_url", artifact.getString("content_url"));
    response.put("created_at", artifact.getString("created_at"));
    response.put("committed_at", artifact.getString("committed_at"));

    ArtifactStatus status;
    try {
      status = ArtifactStatus.valueOf(artifact.getString("status"));
    } catch (Exception e) {
      status = ArtifactStatus.CREATED;
    }
    response.put("_links", LinkBuilder.forArtifact(artifact.getString("id"), status));

    return response;
  }
}
