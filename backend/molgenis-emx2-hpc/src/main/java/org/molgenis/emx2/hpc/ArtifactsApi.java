package org.molgenis.emx2.hpc;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.model.ArtifactStatus;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;
import org.molgenis.emx2.hpc.protocol.InputValidator;
import org.molgenis.emx2.hpc.protocol.LinkBuilder;
import org.molgenis.emx2.hpc.protocol.ProblemDetail;
import org.molgenis.emx2.hpc.service.ArtifactService;

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

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final ArtifactService artifactService;

  public ArtifactsApi(ArtifactService artifactService) {
    this.artifactService = artifactService;
  }

  /** POST /api/hpc/artifacts — create a new artifact. */
  @SuppressWarnings("unchecked")
  public void createArtifact(Context ctx) {
    try {
      Map<String, Object> body = MAPPER.readValue(ctx.body(), Map.class);
      String type = (String) body.get("type");
      String format = (String) body.get("format");
      String residence = (String) body.get("residence");
      String contentUrl = (String) body.get("content_url");
      String metadata =
          body.get("metadata") != null ? MAPPER.writeValueAsString(body.get("metadata")) : null;

      String artifactId =
          artifactService.createArtifact(type, format, residence, contentUrl, metadata);

      boolean isExternal = residence != null && !"managed".equals(residence);
      ArtifactStatus status = isExternal ? ArtifactStatus.REGISTERED : ArtifactStatus.CREATED;

      Map<String, Object> response = new LinkedHashMap<>();
      response.put("id", artifactId);
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
    try {
      String ct = ctx.header("Content-Type");
      boolean isMultipart = ct != null && ct.startsWith("multipart/form-data");

      String path;
      String role;
      String sha256;
      Long sizeBytes;
      String contentType;
      BinaryFileWrapper content = null;

      if (isMultipart) {
        // Configure multipart handling
        File tempFile = File.createTempFile("hpc_upload_", ".tmp");
        tempFile.deleteOnExit();
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
        role = ctx.formParam("role");
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
        role = (String) body.get("role");
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
          artifactService.uploadFile(
              artifactId, path, role, sha256, sizeBytes, contentType, content);

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
    }
  }

  /** GET /api/hpc/artifacts/{id}/files — list files in an artifact. */
  public void listFiles(Context ctx) {
    String artifactId = ctx.pathParam("id");
    try {
      InputValidator.requireUuid(artifactId, "id");
    } catch (IllegalArgumentException e) {
      ProblemDetail.send(
          ctx, 400, "Bad Request", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
      return;
    }
    List<Row> files = artifactService.listFiles(artifactId);

    List<Map<String, Object>> items =
        files.stream()
            .map(
                f -> {
                  Map<String, Object> m = new LinkedHashMap<>();
                  m.put("id", f.getString("id"));
                  m.put("path", f.getString("path"));
                  m.put("role", f.getString("role"));
                  m.put("sha256", f.getString("sha256"));
                  m.put("size_bytes", f.getString("size_bytes"));
                  m.put("content_type", f.getString("content_type"));
                  return m;
                })
            .toList();

    ctx.json(Map.of("items", items, "count", items.size()));
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

      Row committed = artifactService.commitArtifact(artifactId, sha256, sizeBytes);
      if (committed == null) {
        Row existing = artifactService.getArtifact(artifactId);
        if (existing == null) {
          ProblemDetail.send(
              ctx,
              404,
              "Not Found",
              "Artifact " + artifactId + " not found",
              ctx.header(HpcHeaders.REQUEST_ID));
        } else {
          ProblemDetail.send(
              ctx,
              409,
              "Conflict",
              "Artifact "
                  + artifactId
                  + " cannot be committed from status "
                  + existing.getString("status"),
              ctx.header(HpcHeaders.REQUEST_ID));
        }
        return;
      }

      ctx.status(200);
      ctx.json(artifactToResponse(committed));
    } catch (Exception e) {
      ProblemDetail.send(
          ctx, 500, "Internal Server Error", e.getMessage(), ctx.header(HpcHeaders.REQUEST_ID));
    }
  }

  private Map<String, Object> artifactToResponse(Row artifact) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", artifact.getString("id"));
    response.put("type", artifact.getString("type"));
    response.put("format", artifact.getString("format"));
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
