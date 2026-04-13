package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.hpc.service.ArtifactService;
import org.molgenis.emx2.hpc.service.CommitResult;
import org.molgenis.emx2.sql.SqlDatabase;

class ArtifactsApiTest {

  private ArtifactService artifactService;
  private SqlDatabase database;
  private ArtifactsApi artifactsApi;
  private Context ctx;

  private static final String ARTIFACT_ID = UUID.randomUUID().toString();

  @BeforeEach
  void setUp() {
    artifactService = mock(ArtifactService.class);
    database = mock(SqlDatabase.class);
    artifactsApi = new ArtifactsApi(artifactService, database);
    ctx = mock(Context.class);
    when(ctx.header("X-Request-Id")).thenReturn("req-1");
  }

  // ── createArtifact ────────────────────────────────────────────────────────

  @Test
  void createArtifact_managedResidence_statusCreated() throws Exception {
    when(ctx.body())
        .thenReturn("{\"name\":\"output\",\"type\":\"result\",\"residence\":\"managed\"}");
    when(artifactService.createArtifact(eq("output"), eq("result"), eq("managed"), isNull(), any()))
        .thenReturn(ARTIFACT_ID);

    artifactsApi.createArtifact(ctx);

    verify(ctx).status(201);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return ARTIFACT_ID.equals(map.get("id"))
                      && "CREATED".equals(map.get("status"))
                      && "output".equals(map.get("name"));
                }));
  }

  @Test
  void createArtifact_externalResidence_statusRegistered() throws Exception {
    when(ctx.body())
        .thenReturn(
            "{\"name\":\"ext\",\"type\":\"data\",\"residence\":\"posix\",\"content_url\":\"file:///data\"}");
    when(artifactService.createArtifact(
            eq("ext"), eq("data"), eq("posix"), eq("file:///data"), any()))
        .thenReturn(ARTIFACT_ID);

    artifactsApi.createArtifact(ctx);

    verify(ctx).status(201);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return "REGISTERED".equals(map.get("status"));
                }));
  }

  @Test
  void createArtifact_nullResidence_statusCreated() throws Exception {
    when(ctx.body()).thenReturn("{\"name\":\"a\",\"type\":\"t\"}");
    when(artifactService.createArtifact(eq("a"), eq("t"), isNull(), isNull(), any()))
        .thenReturn(ARTIFACT_ID);

    artifactsApi.createArtifact(ctx);

    verify(ctx).status(201);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return "CREATED".equals(map.get("status"));
                }));
  }

  @Test
  void createArtifact_withMetadata() throws Exception {
    when(ctx.body()).thenReturn("{\"name\":\"a\",\"type\":\"t\",\"metadata\":{\"key\":\"value\"}}");
    when(artifactService.createArtifact(eq("a"), eq("t"), isNull(), isNull(), any()))
        .thenReturn(ARTIFACT_ID);

    artifactsApi.createArtifact(ctx);

    verify(ctx).status(201);
    verify(artifactService).createArtifact(eq("a"), eq("t"), isNull(), isNull(), any());
  }

  // ── getArtifact ───────────────────────────────────────────────────────────

  @Test
  void getArtifact_found() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    Row artifact = buildArtifactRow(ARTIFACT_ID, "CREATED", "managed");
    when(artifactService.getArtifact(ARTIFACT_ID)).thenReturn(artifact);

    artifactsApi.getArtifact(ctx);

    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return ARTIFACT_ID.equals(map.get("id"));
                }));
  }

  @Test
  void getArtifact_notFound_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(artifactService.getArtifact(ARTIFACT_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.getArtifact(ctx));
    assertEquals(404, ex.getStatus());
  }

  // ── deleteArtifact ────────────────────────────────────────────────────────

  @Test
  void deleteArtifact_success() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    Row deleted = buildArtifactRow(ARTIFACT_ID, "CREATED", "managed");
    when(artifactService.deleteArtifact(ARTIFACT_ID)).thenReturn(deleted);

    artifactsApi.deleteArtifact(ctx);

    verify(ctx).status(204);
  }

  @Test
  void deleteArtifact_notFound_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(artifactService.deleteArtifact(ARTIFACT_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.deleteArtifact(ctx));
    assertEquals(404, ex.getStatus());
  }

  // ── listFiles ─────────────────────────────────────────────────────────────

  @Test
  void listFiles_basicWithPagination() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.queryParam("prefix")).thenReturn(null);
    when(ctx.queryParam("limit")).thenReturn("10");
    when(ctx.queryParam("offset")).thenReturn("0");

    Row file = new Row();
    file.set("id", "f1");
    file.set("path", "data.txt");
    file.set("sha256", "abc123");
    file.set("size_bytes", "1024");
    file.set("content_type", "text/plain");
    when(artifactService.listFiles(ARTIFACT_ID, null, 10, 0)).thenReturn(List.of(file));
    when(artifactService.countFiles(ARTIFACT_ID, null)).thenReturn(1);

    artifactsApi.listFiles(ctx);

    verify(ctx).header("X-Total-Count", "1");
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return Integer.valueOf(1).equals(map.get("count"))
                      && Integer.valueOf(1).equals(map.get("total_count"))
                      && Integer.valueOf(10).equals(map.get("limit"))
                      && Integer.valueOf(0).equals(map.get("offset"));
                }));
  }

  @Test
  void listFiles_defaultLimitAndOffset() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.queryParam("prefix")).thenReturn(null);
    when(ctx.queryParam("limit")).thenReturn(null);
    when(ctx.queryParam("offset")).thenReturn(null);
    when(artifactService.listFiles(ARTIFACT_ID, null, 100, 0)).thenReturn(List.of());
    when(artifactService.countFiles(ARTIFACT_ID, null)).thenReturn(0);

    artifactsApi.listFiles(ctx);

    verify(artifactService).listFiles(ARTIFACT_ID, null, 100, 0);
  }

  @Test
  void listFiles_withPrefix() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.queryParam("prefix")).thenReturn("logs/");
    when(ctx.queryParam("limit")).thenReturn("50");
    when(ctx.queryParam("offset")).thenReturn("5");
    when(artifactService.listFiles(ARTIFACT_ID, "logs/", 50, 5)).thenReturn(List.of());
    when(artifactService.countFiles(ARTIFACT_ID, "logs/")).thenReturn(0);

    artifactsApi.listFiles(ctx);

    verify(artifactService).listFiles(ARTIFACT_ID, "logs/", 50, 5);
    verify(artifactService).countFiles(ARTIFACT_ID, "logs/");
  }

  // ── commitArtifact ────────────────────────────────────────────────────────

  @Test
  void commitArtifact_success() throws Exception {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.body()).thenReturn("{\"sha256\":\"abc123\",\"size_bytes\":4096}");
    Row committedRow = buildArtifactRow(ARTIFACT_ID, "COMMITTED", "managed");
    when(artifactService.commitArtifact(ARTIFACT_ID, "abc123", 4096L))
        .thenReturn(CommitResult.success(committedRow));

    artifactsApi.commitArtifact(ctx);

    verify(ctx).status(200);
    verify(ctx).json(any());
  }

  @Test
  void commitArtifact_notFound_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.body()).thenReturn("{\"sha256\":\"abc123\"}");
    when(artifactService.commitArtifact(ARTIFACT_ID, "abc123", null)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.commitArtifact(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void commitArtifact_hashMismatch_throws409() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.body()).thenReturn("{\"sha256\":\"wrong\"}");
    when(artifactService.commitArtifact(ARTIFACT_ID, "wrong", null))
        .thenReturn(CommitResult.hashMismatch("Expected abc123 but got wrong"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.commitArtifact(ctx));
    assertEquals(409, ex.getStatus());
    assertEquals("Hash Mismatch", ex.getTitle());
  }

  @Test
  void commitArtifact_wrongState_throws409() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.body()).thenReturn("{\"sha256\":\"abc123\"}");
    when(artifactService.commitArtifact(ARTIFACT_ID, "abc123", null))
        .thenReturn(CommitResult.wrongState("Artifact is already committed"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.commitArtifact(ctx));
    assertEquals(409, ex.getStatus());
    assertEquals("Conflict", ex.getTitle());
  }

  @Test
  void commitArtifact_nullSizeBytes() throws Exception {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.body()).thenReturn("{\"sha256\":\"abc123\"}");
    Row committedRow = buildArtifactRow(ARTIFACT_ID, "COMMITTED", "managed");
    when(artifactService.commitArtifact(ARTIFACT_ID, "abc123", null))
        .thenReturn(CommitResult.success(committedRow));

    artifactsApi.commitArtifact(ctx);

    verify(artifactService).commitArtifact(ARTIFACT_ID, "abc123", null);
    verify(ctx).status(200);
  }

  // ── headFile ──────────────────────────────────────────────────────────────

  @Test
  void headFile_found_setsHeaders() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");

    Row file = new Row();
    file.set("sha256", "abc123");
    file.set("size_bytes", "1024");
    file.set("content_type", "text/plain");
    when(artifactService.getFileMetadata(ARTIFACT_ID, "data.txt")).thenReturn(file);

    artifactsApi.headFile(ctx);

    verify(ctx).header("X-Content-SHA256", "abc123");
    verify(ctx).header("Content-Length", "1024");
    verify(ctx).header("Content-Type", "text/plain");
    verify(ctx).status(200);
  }

  @Test
  void headFile_found_nullFields_skipsHeaders() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");

    Row file = new Row();
    // All fields null — no headers set
    when(artifactService.getFileMetadata(ARTIFACT_ID, "data.txt")).thenReturn(file);

    artifactsApi.headFile(ctx);

    verify(ctx, never()).header(eq("X-Content-SHA256"), anyString());
    verify(ctx, never()).header(eq("Content-Length"), anyString());
    verify(ctx, never()).header(eq("Content-Type"), anyString());
    verify(ctx).status(200);
  }

  @Test
  void headFile_notFound_returns404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("missing.txt");
    when(artifactService.getFileMetadata(ARTIFACT_ID, "missing.txt")).thenReturn(null);

    artifactsApi.headFile(ctx);

    verify(ctx).status(404);
  }

  // ── deleteFile ────────────────────────────────────────────────────────────

  @Test
  void deleteFile_success() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");
    when(artifactService.deleteFile(ARTIFACT_ID, "data.txt")).thenReturn(true);

    artifactsApi.deleteFile(ctx);

    verify(ctx).status(204);
  }

  @Test
  void deleteFile_notFound_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("missing.txt");
    when(artifactService.deleteFile(ARTIFACT_ID, "missing.txt")).thenReturn(false);

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.deleteFile(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void deleteFile_committed_throws409() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");
    when(artifactService.deleteFile(ARTIFACT_ID, "data.txt"))
        .thenThrow(new MolgenisException("Artifact is committed"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.deleteFile(ctx));
    assertEquals(409, ex.getStatus());
  }

  @Test
  void deleteFile_molgenisExceptionNonCommitted_throws500() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");
    when(artifactService.deleteFile(ARTIFACT_ID, "data.txt"))
        .thenThrow(new MolgenisException("Some other DB error"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.deleteFile(ctx));
    assertEquals(500, ex.getStatus());
  }

  // ── downloadFile ──────────────────────────────────────────────────────────

  @Test
  void downloadFile_fileNull_artifactHasContentUrl_redirects() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.txt")).thenReturn(null);

    Row artifact = new Row();
    artifact.set("content_url", "https://storage.example.com/bucket");
    when(artifactService.getArtifact(ARTIFACT_ID)).thenReturn(artifact);

    artifactsApi.downloadFile(ctx);

    verify(ctx).redirect("https://storage.example.com/bucket/data.txt");
  }

  @Test
  void downloadFile_fileNull_noContentUrl_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.txt")).thenReturn(null);

    Row artifact = new Row();
    // no content_url set
    when(artifactService.getArtifact(ARTIFACT_ID)).thenReturn(artifact);

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.downloadFile(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void downloadFile_fileNull_noArtifact_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.txt")).thenReturn(null);
    when(artifactService.getArtifact(ARTIFACT_ID)).thenReturn(null);

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.downloadFile(ctx));
    assertEquals(404, ex.getStatus());
  }

  @Test
  void downloadFile_fileHasNullBytes_artifactHasContentUrl_redirects() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");

    Row file = mock(Row.class);
    when(file.getBinary("content_contents")).thenReturn(null);
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.txt")).thenReturn(file);

    Row artifact = new Row();
    artifact.set("content_url", "https://storage.example.com/bucket");
    when(artifactService.getArtifact(ARTIFACT_ID)).thenReturn(artifact);

    artifactsApi.downloadFile(ctx);

    verify(ctx).redirect("https://storage.example.com/bucket/data.txt");
  }

  @Test
  void downloadFile_fileHasNullBytes_noContentUrl_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");

    Row file = mock(Row.class);
    when(file.getBinary("content_contents")).thenReturn(null);
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.txt")).thenReturn(file);

    Row artifact = new Row();
    when(artifactService.getArtifact(ARTIFACT_ID)).thenReturn(artifact);

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.downloadFile(ctx));
    assertEquals(404, ex.getStatus());
    assertTrue(ex.getMessage().contains("has no content"));
  }

  @Test
  void downloadFile_fileHasBytes_returnsContent() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");

    byte[] content = new byte[] {1, 2, 3};
    Row file = mock(Row.class);
    when(file.getBinary("content_contents")).thenReturn(content);
    when(file.getString("content_mimetype")).thenReturn("text/plain");
    when(file.getString("sha256")).thenReturn("abc123");
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.txt")).thenReturn(file);

    artifactsApi.downloadFile(ctx);

    verify(ctx).header("Content-Type", "text/plain");
    verify(ctx).header("X-Content-SHA256", "abc123");
    verify(ctx).header("Content-Length", "3");
    verify(ctx).result(content);
  }

  @Test
  void downloadFile_fileHasBytes_nullMimetype_fallsToContentType() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.bin");

    byte[] content = new byte[] {1, 2, 3};
    Row file = mock(Row.class);
    when(file.getBinary("content_contents")).thenReturn(content);
    when(file.getString("content_mimetype")).thenReturn(null);
    when(file.getString("content_type")).thenReturn("application/pdf");
    when(file.getString("sha256")).thenReturn(null);
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.bin")).thenReturn(file);

    artifactsApi.downloadFile(ctx);

    verify(ctx).header("Content-Type", "application/pdf");
    verify(ctx, never()).header(eq("X-Content-SHA256"), anyString());
  }

  @Test
  void downloadFile_fileHasBytes_allNullTypes_fallsToOctetStream() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.bin");

    byte[] content = new byte[] {1, 2, 3};
    Row file = mock(Row.class);
    when(file.getBinary("content_contents")).thenReturn(content);
    when(file.getString("content_mimetype")).thenReturn(null);
    when(file.getString("content_type")).thenReturn(null);
    when(file.getString("sha256")).thenReturn(null);
    when(artifactService.getFileWithContent(ARTIFACT_ID, "data.bin")).thenReturn(file);

    artifactsApi.downloadFile(ctx);

    verify(ctx).header("Content-Type", "application/octet-stream");
  }

  // ── getMaxUploadBytes ──────────────────────────────────────────────────────

  @Test
  void getMaxUploadBytes_default_whenSettingNull() {
    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);
      assertEquals(ArtifactsApi.DEFAULT_MAX_UPLOAD_BYTES, artifactsApi.getMaxUploadBytes());
    }
  }

  @Test
  void getMaxUploadBytes_default_whenSettingBlank() {
    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("  ");
      assertEquals(ArtifactsApi.DEFAULT_MAX_UPLOAD_BYTES, artifactsApi.getMaxUploadBytes());
    }
  }

  @Test
  void getMaxUploadBytes_customValue() {
    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("1048576");
      assertEquals(1048576L, artifactsApi.getMaxUploadBytes());
    }
  }

  @Test
  void getMaxUploadBytes_invalidValue_fallsBackToDefault() {
    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("not-a-number");
      assertEquals(ArtifactsApi.DEFAULT_MAX_UPLOAD_BYTES, artifactsApi.getMaxUploadBytes());
    }
  }

  @Test
  void getMaxUploadBytes_negativeValue_fallsBackToDefault() {
    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("-100");
      assertEquals(ArtifactsApi.DEFAULT_MAX_UPLOAD_BYTES, artifactsApi.getMaxUploadBytes());
    }
  }

  @Test
  void getMaxUploadBytes_zeroValue_fallsBackToDefault() {
    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("0");
      assertEquals(ArtifactsApi.DEFAULT_MAX_UPLOAD_BYTES, artifactsApi.getMaxUploadBytes());
    }
  }

  @Test
  void getMaxUploadBytes_valueWithWhitespace_trims() {
    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("  2097152  ");
      assertEquals(2097152L, artifactsApi.getMaxUploadBytes());
    }
  }

  // ── uploadFileByPath (JSON metadata mode) ─────────────────────────────────

  @Test
  void uploadFileByPath_jsonMetadata_success() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.json");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body())
        .thenReturn("{\"sha256\":\"abc123\",\"size_bytes\":100,\"content_type\":\"text/plain\"}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID), eq("data.json"), eq("abc123"), eq(100L), eq("text/plain"), isNull()))
        .thenReturn("file-1");

    artifactsApi.uploadFileByPath(ctx);

    verify(ctx).status(201);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return "file-1".equals(map.get("id"))
                      && ARTIFACT_ID.equals(map.get("artifact_id"))
                      && "data.json".equals(map.get("path"))
                      && "abc123".equals(map.get("sha256"))
                      && Long.valueOf(100L).equals(map.get("size_bytes"));
                }));
  }

  @Test
  void uploadFileByPath_jsonMetadata_nullContentType_defaultsToOctetStream() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.bin");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body()).thenReturn("{\"sha256\":\"def456\",\"size_bytes\":200}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID),
            eq("data.bin"),
            eq("def456"),
            eq(200L),
            eq("application/octet-stream"),
            isNull()))
        .thenReturn("file-2");

    artifactsApi.uploadFileByPath(ctx);

    verify(ctx).status(201);
    verify(artifactService)
        .uploadFileByPath(
            eq(ARTIFACT_ID),
            eq("data.bin"),
            eq("def456"),
            eq(200L),
            eq("application/octet-stream"),
            isNull());
  }

  @Test
  void uploadFileByPath_jsonMetadata_blankContentType_defaultsToOctetStream() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.bin");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body())
        .thenReturn("{\"sha256\":\"def456\",\"size_bytes\":50,\"content_type\":\"  \"}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID),
            eq("data.bin"),
            eq("def456"),
            eq(50L),
            eq("application/octet-stream"),
            isNull()))
        .thenReturn("file-3");

    artifactsApi.uploadFileByPath(ctx);

    verify(ctx).status(201);
  }

  @Test
  void uploadFileByPath_jsonMetadata_nullSizeBytes_defaultsToZero() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.txt");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body()).thenReturn("{\"sha256\":\"abc\",\"content_type\":\"text/plain\"}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID), eq("data.txt"), eq("abc"), eq(0L), eq("text/plain"), isNull()))
        .thenReturn("file-4");

    artifactsApi.uploadFileByPath(ctx);

    verify(ctx).status(201);
    verify(artifactService)
        .uploadFileByPath(
            eq(ARTIFACT_ID), eq("data.txt"), eq("abc"), eq(0L), eq("text/plain"), isNull());
  }

  // ── uploadFileByPath error paths ──────────────────────────────────────────

  @Test
  void uploadFileByPath_artifactNotFound_throws404() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.json");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body())
        .thenReturn("{\"sha256\":\"abc\",\"size_bytes\":100,\"content_type\":\"text/plain\"}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID), eq("data.json"), eq("abc"), eq(100L), eq("text/plain"), isNull()))
        .thenThrow(new MolgenisException("Artifact " + ARTIFACT_ID + " not found"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
    assertEquals(404, ex.getStatus());
    assertTrue(ex.getMessage().contains("not found"));
  }

  @Test
  void uploadFileByPath_genericMolgenisException_throws500() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.json");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body())
        .thenReturn("{\"sha256\":\"abc\",\"size_bytes\":100,\"content_type\":\"text/plain\"}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID), eq("data.json"), eq("abc"), eq(100L), eq("text/plain"), isNull()))
        .thenThrow(new MolgenisException("DB constraint violated"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
    assertEquals(500, ex.getStatus());
  }

  @Test
  void uploadFileByPath_genericException_throws500() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.json");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body())
        .thenReturn("{\"sha256\":\"abc\",\"size_bytes\":100,\"content_type\":\"text/plain\"}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID), eq("data.json"), eq("abc"), eq(100L), eq("text/plain"), isNull()))
        .thenThrow(new RuntimeException("unexpected IO error"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
    assertEquals(500, ex.getStatus());
  }

  // ── uploadFileByPath (binary upload mode) ──────────────────────────────────

  @Test
  void uploadFileByPath_binaryUpload_success() throws Exception {
    byte[] content = "hello world".getBytes();
    String sha256 = computeSha256(content);

    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("output.txt");
    when(ctx.header("Content-Type")).thenReturn("text/plain");
    when(ctx.header("Content-Length")).thenReturn(String.valueOf(content.length));
    when(ctx.header("Content-SHA256")).thenReturn(sha256);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getInputStream()).thenReturn(toServletInputStream(content));

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);

      when(artifactService.uploadFileByPath(
              eq(ARTIFACT_ID),
              eq("output.txt"),
              eq(sha256),
              eq((long) content.length),
              anyString(),
              any()))
          .thenReturn("file-bin-1");

      artifactsApi.uploadFileByPath(ctx);
    }

    verify(ctx).status(201);
    verify(ctx)
        .json(
            argThat(
                obj -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> map = (Map<String, Object>) obj;
                  return "file-bin-1".equals(map.get("id"))
                      && sha256.equals(map.get("sha256"))
                      && Long.valueOf(content.length).equals(map.get("size_bytes"));
                }));
  }

  @Test
  void uploadFileByPath_binaryUpload_contentLengthExceedsLimit_throws413() throws Exception {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("big.bin");
    when(ctx.header("Content-Type")).thenReturn("application/octet-stream");
    when(ctx.header("Content-Length")).thenReturn("999999999999");

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getInputStream()).thenReturn(toServletInputStream(new byte[0]));

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("1024");

      HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
      assertEquals(413, ex.getStatus());
      assertTrue(ex.getMessage().contains("exceeds maximum"));
    }
  }

  @Test
  void uploadFileByPath_binaryUpload_unparseableContentLength_proceedsToStream() throws Exception {
    byte[] content = "data".getBytes();
    String sha256 = computeSha256(content);

    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("file.dat");
    when(ctx.header("Content-Type")).thenReturn(null); // no content type
    when(ctx.header("Content-Length")).thenReturn("not-a-number");
    when(ctx.header("Content-SHA256")).thenReturn(sha256);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getInputStream()).thenReturn(toServletInputStream(content));

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);

      when(artifactService.uploadFileByPath(
              eq(ARTIFACT_ID),
              eq("file.dat"),
              eq(sha256),
              eq((long) content.length),
              eq("application/octet-stream"),
              any()))
          .thenReturn("file-bin-2");

      artifactsApi.uploadFileByPath(ctx);
    }

    verify(ctx).status(201);
  }

  @Test
  void uploadFileByPath_binaryUpload_streamExceedsLimit_throws413() throws Exception {
    // Content is larger than the max upload limit
    byte[] content = new byte[2048];
    java.util.Arrays.fill(content, (byte) 'x');

    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("big.bin");
    when(ctx.header("Content-Type")).thenReturn("application/octet-stream");
    when(ctx.header("Content-Length")).thenReturn(null); // no declared length

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getInputStream()).thenReturn(toServletInputStream(content));

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("1024"); // limit is 1024 but content is 2048

      HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
      assertEquals(413, ex.getStatus());
      assertTrue(ex.getMessage().contains("exceeds maximum"));
    }
  }

  @Test
  void uploadFileByPath_binaryUpload_guessesContentTypeFromFilename() throws Exception {
    byte[] content = "<html></html>".getBytes();
    String sha256 = computeSha256(content);

    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("page.html");
    when(ctx.header("Content-Type")).thenReturn("application/octet-stream");
    when(ctx.header("Content-Length")).thenReturn(String.valueOf(content.length));
    when(ctx.header("Content-SHA256")).thenReturn(sha256);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getInputStream()).thenReturn(toServletInputStream(content));

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);

      when(artifactService.uploadFileByPath(
              eq(ARTIFACT_ID),
              eq("page.html"),
              eq(sha256),
              eq((long) content.length),
              eq("text/html"),
              any()))
          .thenReturn("file-html");

      artifactsApi.uploadFileByPath(ctx);
    }

    // resolveContentType should guess text/html from .html extension
    verify(artifactService)
        .uploadFileByPath(
            eq(ARTIFACT_ID),
            eq("page.html"),
            eq(sha256),
            eq((long) content.length),
            eq("text/html"),
            any());
  }

  @Test
  void uploadFileByPath_binaryUpload_unknownExtension_usesDeclaredType() throws Exception {
    byte[] content = "data".getBytes();
    String sha256 = computeSha256(content);

    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("file.xyz123");
    when(ctx.header("Content-Type")).thenReturn("application/custom");
    when(ctx.header("Content-Length")).thenReturn(String.valueOf(content.length));
    when(ctx.header("Content-SHA256")).thenReturn(sha256);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getInputStream()).thenReturn(toServletInputStream(content));

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);

      when(artifactService.uploadFileByPath(
              eq(ARTIFACT_ID),
              eq("file.xyz123"),
              eq(sha256),
              eq((long) content.length),
              eq("application/custom"),
              any()))
          .thenReturn("file-custom");

      artifactsApi.uploadFileByPath(ctx);
    }

    verify(artifactService)
        .uploadFileByPath(
            eq(ARTIFACT_ID),
            eq("file.xyz123"),
            eq(sha256),
            eq((long) content.length),
            eq("application/custom"),
            any());
  }

  @Test
  void uploadFileByPath_binaryUpload_noContentTypeNoGuess_defaultsToOctetStream() throws Exception {
    byte[] content = "data".getBytes();
    String sha256 = computeSha256(content);

    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("file.xyz123");
    when(ctx.header("Content-Type")).thenReturn(null);
    when(ctx.header("Content-Length")).thenReturn(String.valueOf(content.length));
    when(ctx.header("Content-SHA256")).thenReturn(sha256);

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getInputStream()).thenReturn(toServletInputStream(content));

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);

      when(artifactService.uploadFileByPath(
              eq(ARTIFACT_ID),
              eq("file.xyz123"),
              eq(sha256),
              eq((long) content.length),
              eq("application/octet-stream"),
              any()))
          .thenReturn("file-default");

      artifactsApi.uploadFileByPath(ctx);
    }

    verify(artifactService)
        .uploadFileByPath(
            eq(ARTIFACT_ID),
            eq("file.xyz123"),
            eq(sha256),
            eq((long) content.length),
            eq("application/octet-stream"),
            any());
  }

  // ── uploadFileByPath (multipart upload mode) ───────────────────────────────

  @Test
  void uploadFileByPath_multipartUpload_success() throws Exception {
    byte[] content = "multipart data".getBytes();
    String sha256 = computeSha256(content);

    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("upload.txt");
    when(ctx.header("Content-Type")).thenReturn("multipart/form-data; boundary=abc");
    when(ctx.header("Content-Length")).thenReturn(String.valueOf(content.length));
    when(ctx.header("Content-SHA256")).thenReturn(sha256);
    when(ctx.formParam("content_type")).thenReturn("text/plain");

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);

    Part filePart = mock(Part.class);
    when(filePart.getInputStream()).thenReturn(new ByteArrayInputStream(content));
    when(filePart.getContentType()).thenReturn("application/octet-stream");
    when(req.getPart("file")).thenReturn(filePart);

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);

      when(artifactService.uploadFileByPath(
              eq(ARTIFACT_ID),
              eq("upload.txt"),
              eq(sha256),
              eq((long) content.length),
              anyString(),
              any()))
          .thenReturn("file-mp-1");

      artifactsApi.uploadFileByPath(ctx);
    }

    verify(ctx).status(201);
  }

  @Test
  void uploadFileByPath_multipartUpload_missingFilePart_throws400() throws Exception {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("upload.txt");
    when(ctx.header("Content-Type")).thenReturn("multipart/form-data; boundary=abc");
    when(ctx.header("Content-Length")).thenReturn("100");

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);
    when(req.getPart("file")).thenReturn(null);

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn(null);

      HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
      assertEquals(400, ex.getStatus());
      assertTrue(ex.getMessage().contains("Multipart"));
    }
  }

  @Test
  void uploadFileByPath_multipartUpload_contentLengthExceedsLimit_throws413() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("upload.txt");
    when(ctx.header("Content-Type")).thenReturn("multipart/form-data; boundary=abc");
    when(ctx.header("Content-Length")).thenReturn("999999999");

    HttpServletRequest req = mock(HttpServletRequest.class);
    when(ctx.req()).thenReturn(req);

    try (MockedStatic<HpcAuth> mockedAuth = mockStatic(HpcAuth.class)) {
      mockedAuth
          .when(() -> HpcAuth.readSetting(database, ArtifactsApi.MAX_UPLOAD_BYTES_SETTING))
          .thenReturn("1024");

      HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
      assertEquals(413, ex.getStatus());
    }
  }

  @Test
  void uploadFileByPath_hpcException_rethrown() {
    when(ctx.pathParam("id")).thenReturn(ARTIFACT_ID);
    when(ctx.pathParam("path")).thenReturn("data.json");
    when(ctx.header("Content-Type")).thenReturn("application/json");
    when(ctx.body())
        .thenReturn("{\"sha256\":\"abc\",\"size_bytes\":100,\"content_type\":\"text/plain\"}");

    when(artifactService.uploadFileByPath(
            eq(ARTIFACT_ID), eq("data.json"), eq("abc"), eq(100L), eq("text/plain"), isNull()))
        .thenThrow(new HpcException(409, "Conflict", "Artifact is committed", "req-1"));

    HpcException ex = assertThrows(HpcException.class, () -> artifactsApi.uploadFileByPath(ctx));
    assertEquals(409, ex.getStatus());
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private static String computeSha256(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(data));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static ServletInputStream toServletInputStream(byte[] data) {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    return new ServletInputStream() {
      @Override
      public int read() throws IOException {
        return bais.read();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        return bais.read(b, off, len);
      }

      @Override
      public boolean isFinished() {
        return bais.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        // No-op: tests use blocking reads, so non-blocking I/O listeners are never invoked.
      }
    };
  }

  private static Row buildArtifactRow(String id, String status, String residence) {
    Row row = new Row();
    row.set("id", id);
    row.set("name", "test-artifact");
    row.set("type", "result");
    row.set("residence", residence);
    row.set("status", status);
    row.set("sha256", null);
    row.set("size_bytes", null);
    row.set("content_url", null);
    row.set("created_at", "2025-01-01T00:00:00");
    row.set("committed_at", null);
    return row;
  }
}
