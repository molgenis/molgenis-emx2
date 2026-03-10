package org.molgenis.emx2.hpc;

import io.javalin.http.Context;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.molgenis.emx2.hpc.protocol.HpcHeaders;

/** Shared static helpers used by both {@link JobsApi} and {@link ArtifactsApi}. */
final class HpcApiUtils {

  private HpcApiUtils() {}

  /** Extract the optional request-id header (used for error correlation). */
  static String requestId(Context ctx) {
    return ctx.header(HpcHeaders.REQUEST_ID);
  }

  /**
   * Build a RFC 6266 Content-Disposition header with both a quoted fallback and a UTF-8 extended
   * value.
   */
  static String buildContentDispositionHeader(String fileName) {
    String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    return "attachment; filename=\""
        + escapeQuotedHeaderValue(fileName)
        + "\"; filename*=UTF-8''"
        + encoded;
  }

  /**
   * Strips control characters from a file path, returning only the base name. Falls back to {@code
   * "download"} when the result would be empty.
   */
  static String sanitizeDownloadFileName(String filePath) {
    String normalized = filePath == null ? "" : filePath.replace('\\', '/');
    String baseName = normalized.substring(normalized.lastIndexOf('/') + 1);
    StringBuilder safe = new StringBuilder(baseName.length());
    for (int i = 0; i < baseName.length(); i++) {
      char ch = baseName.charAt(i);
      if (ch < 0x20 || ch == 0x7f) {
        continue;
      }
      safe.append(ch);
    }
    String result = safe.toString().trim();
    return result.isEmpty() ? "download" : result;
  }

  /** Escape double-quote and backslash for use inside a quoted HTTP header value. */
  static String escapeQuotedHeaderValue(String value) {
    StringBuilder escaped = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char ch = value.charAt(i);
      if (ch == '"' || ch == '\\') {
        escaped.append('\\');
      }
      escaped.append(ch);
    }
    return escaped.toString();
  }

  /**
   * Verifies that the Content-SHA256 header (if present) matches the actual SHA-256 of the received
   * bytes. This closes the MITM window: the HMAC proved the client intended this hash, and this
   * check verifies the bytes match.
   */
  static void verifyContentSha256(Context ctx, String actualSha256) {
    String claimedHash = ctx.header(HpcHeaders.CONTENT_SHA256);
    if (claimedHash != null && !claimedHash.isBlank() && !actualSha256.equals(claimedHash)) {
      throw HpcException.badRequest(
          "Content-SHA256 header does not match actual content hash"
              + " (claimed="
              + claimedHash
              + ", actual="
              + actualSha256
              + ")",
          requestId(ctx));
    }
  }
}
