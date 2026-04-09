package org.molgenis.emx2.hpc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;

class HpcApiUtilsTest {

  @ParameterizedTest
  @CsvSource({
    "results/data.txt, data.txt",
    "data.txt, data.txt",
    "nested/deep/file.csv, file.csv",
    "backslash\\path\\file.bin, file.bin",
    "mixed/back\\slash.txt, slash.txt",
  })
  void sanitizeDownloadFileName_extractsBaseName(String input, String expected) {
    assertEquals(expected, HpcApiUtils.sanitizeDownloadFileName(input));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"/", "\\", "///", "\u0001\u0002"})
  void sanitizeDownloadFileName_fallsBackToDownload(String input) {
    assertEquals("download", HpcApiUtils.sanitizeDownloadFileName(input));
  }

  @Test
  void sanitizeDownloadFileName_stripsControlCharacters() {
    assertEquals("clean.txt", HpcApiUtils.sanitizeDownloadFileName("cle\u0001an\u007f.txt"));
  }

  @Test
  void buildContentDispositionHeader_rfc6266Format() {
    String header = HpcApiUtils.buildContentDispositionHeader("report.pdf");
    assertTrue(header.startsWith("attachment; filename=\"report.pdf\""));
    assertTrue(header.contains("filename*=UTF-8''report.pdf"));
  }

  @Test
  void buildContentDispositionHeader_encodesSpecialCharacters() {
    String header = HpcApiUtils.buildContentDispositionHeader("my file (1).txt");
    assertTrue(header.contains("filename*=UTF-8''my%20file%20%281%29.txt"));
  }

  @Test
  void escapeQuotedHeaderValue_escapesQuotesAndBackslashes() {
    assertEquals("no special", HpcApiUtils.escapeQuotedHeaderValue("no special"));
    assertEquals("has \\\"quote\\\"", HpcApiUtils.escapeQuotedHeaderValue("has \"quote\""));
    assertEquals("back\\\\slash", HpcApiUtils.escapeQuotedHeaderValue("back\\slash"));
  }

  // ── requestId ─────────────────────────────────────────────────────────────

  @Test
  void requestId_returnsHeaderValue() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-42");
    assertEquals("req-42", HpcApiUtils.requestId(ctx));
  }

  @Test
  void requestId_returnsNullWhenMissing() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn(null);
    assertNull(HpcApiUtils.requestId(ctx));
  }

  // ── verifyContentSha256 ───────────────────────────────────────────────────

  @Test
  void verifyContentSha256_validMatch_doesNotThrow() {
    Context ctx = mock(Context.class);
    String hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn(hash);
    assertDoesNotThrow(() -> HpcApiUtils.verifyContentSha256(ctx, hash));
  }

  @Test
  void verifyContentSha256_caseInsensitiveMatch_doesNotThrow() {
    Context ctx = mock(Context.class);
    String hashLower = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    String hashUpper = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn(hashUpper);
    assertDoesNotThrow(() -> HpcApiUtils.verifyContentSha256(ctx, hashLower));
  }

  @Test
  void verifyContentSha256_missingHeader_throws400() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn(null);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");

    HpcException ex =
        assertThrows(HpcException.class, () -> HpcApiUtils.verifyContentSha256(ctx, "abc"));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("Content-SHA256 header is required"));
  }

  @Test
  void verifyContentSha256_blankHeader_throws400() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn("   ");
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");

    HpcException ex =
        assertThrows(HpcException.class, () -> HpcApiUtils.verifyContentSha256(ctx, "abc"));
    assertEquals(400, ex.getStatus());
  }

  @Test
  void verifyContentSha256_invalidFormat_throws400() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn("not-hex-64-chars");
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");

    HpcException ex =
        assertThrows(HpcException.class, () -> HpcApiUtils.verifyContentSha256(ctx, "abc"));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("64-character hex string"));
  }

  @Test
  void verifyContentSha256_tooShort_throws400() {
    Context ctx = mock(Context.class);
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn("abcdef1234");
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");

    HpcException ex =
        assertThrows(HpcException.class, () -> HpcApiUtils.verifyContentSha256(ctx, "abc"));
    assertEquals(400, ex.getStatus());
  }

  @Test
  void verifyContentSha256_mismatch_throws400() {
    Context ctx = mock(Context.class);
    String claimed = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    String actual = "a3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn(claimed);
    when(ctx.header(HpcHeaders.REQUEST_ID)).thenReturn("req-1");

    HpcException ex =
        assertThrows(HpcException.class, () -> HpcApiUtils.verifyContentSha256(ctx, actual));
    assertEquals(400, ex.getStatus());
    assertTrue(ex.getMessage().contains("does not match"));
    assertTrue(ex.getMessage().contains("claimed=" + claimed));
    assertTrue(ex.getMessage().contains("actual=" + actual));
  }

  @Test
  void verifyContentSha256_headerWithWhitespace_trims() {
    Context ctx = mock(Context.class);
    String hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    when(ctx.header(HpcHeaders.CONTENT_SHA256)).thenReturn("  " + hash + "  ");
    assertDoesNotThrow(() -> HpcApiUtils.verifyContentSha256(ctx, hash));
  }
}
