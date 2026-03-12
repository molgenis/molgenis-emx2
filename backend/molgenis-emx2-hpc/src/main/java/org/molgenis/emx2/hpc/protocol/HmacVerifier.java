package org.molgenis.emx2.hpc.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HMAC-SHA256 request verification for the HPC bridge protocol.
 *
 * <p>Verifies incoming requests by reconstructing the canonical request string from the HTTP
 * request, computing the HMAC-SHA256 signature, and comparing it to the signature in the
 * Authorization header.
 *
 * <p>Also provides nonce replay protection (time-based + bounded LRU eviction) and timestamp drift
 * validation.
 *
 * <p>For non-JSON request bodies (binary uploads), the client MUST send a {@code Content-SHA256}
 * header containing the hex-encoded SHA-256 of the body. This value is used directly as the body
 * hash in the canonical string, providing end-to-end integrity without requiring the server to
 * buffer the entire body before verifying the signature.
 */
public final class HmacVerifier {

  private static final Logger logger = LoggerFactory.getLogger(HmacVerifier.class);
  private static final String ALGORITHM = "HmacSHA256";
  private static final long MAX_TIMESTAMP_DRIFT_SECONDS = 300; // 5 minutes
  private static final int DEFAULT_MAX_NONCE_CACHE_ENTRIES = 50_000;

  private final String sharedSecret;
  private final int maxNonceCacheEntries;
  private final Object nonceLock = new Object();

  /** Time-based nonce cache with LRU eviction to keep memory bounded. */
  private final LinkedHashMap<String, Long> seenNonces = new LinkedHashMap<>(128, 0.75f, true);

  public HmacVerifier(String sharedSecret) {
    this(sharedSecret, DEFAULT_MAX_NONCE_CACHE_ENTRIES);
  }

  HmacVerifier(String sharedSecret, int maxNonceCacheEntries) {
    if (sharedSecret == null || sharedSecret.length() < 32) {
      throw new IllegalArgumentException(
          "Shared secret must be at least 32 characters for adequate security");
    }
    if (maxNonceCacheEntries < 1) {
      throw new IllegalArgumentException("maxNonceCacheEntries must be >= 1");
    }
    this.sharedSecret = sharedSecret;
    this.maxNonceCacheEntries = maxNonceCacheEntries;
  }

  /**
   * Verifies an incoming request. Checks:
   *
   * <ol>
   *   <li>Authorization header present and prefixed with "HMAC-SHA256 "
   *   <li>Timestamp within acceptable drift window
   *   <li>Nonce not previously seen (replay protection)
   *   <li>HMAC signature matches
   * </ol>
   *
   * @param contentSha256 hex-encoded SHA-256 of the request body, provided via the {@code
   *     Content-SHA256} header for non-JSON bodies. When non-null, this value is used directly as
   *     the body hash in the canonical string instead of hashing {@code body}.
   * @throws SecurityException if verification fails
   */
  public void verify(
      String method,
      String path,
      String body,
      String authHeader,
      String timestamp,
      String nonce,
      String contentSha256) {
    // 1. Parse authorization header
    if (authHeader == null || !authHeader.startsWith("HMAC-SHA256 ")) {
      throw new SecurityException("Missing or invalid Authorization header");
    }
    String providedSignature = authHeader.substring("HMAC-SHA256 ".length()).trim();

    // 2. Validate timestamp
    if (timestamp == null || timestamp.isBlank()) {
      throw new SecurityException("Missing X-Timestamp header");
    }
    long requestTime;
    try {
      requestTime = Long.parseLong(timestamp);
    } catch (NumberFormatException e) {
      throw new SecurityException("Invalid X-Timestamp format");
    }
    long now = System.currentTimeMillis() / 1000;
    if (Math.abs(now - requestTime) > MAX_TIMESTAMP_DRIFT_SECONDS) {
      throw new SecurityException(
          "Request timestamp too far from server time (drift: " + (now - requestTime) + "s)");
    }

    // 3. Nonce replay check (bounded LRU + expiry)
    if (nonce == null || nonce.isBlank()) {
      throw new SecurityException("Missing X-Nonce header");
    }
    synchronized (nonceLock) {
      evictExpiredNonces(now - MAX_TIMESTAMP_DRIFT_SECONDS);
      if (seenNonces.containsKey(nonce)) {
        throw new SecurityException("Duplicate nonce (possible replay attack)");
      }
      seenNonces.put(nonce, requestTime);
      evictLruIfNeeded();
    }

    // 4. Compute and verify signature
    String expectedSignature =
        computeSignature(method, path, body, timestamp, nonce, contentSha256);
    if (!MessageDigest.isEqual(
        expectedSignature.getBytes(StandardCharsets.UTF_8),
        providedSignature.getBytes(StandardCharsets.UTF_8))) {
      throw new SecurityException("HMAC signature mismatch");
    }
  }

  /**
   * Computes the HMAC-SHA256 signature for a request using the canonical format:
   * METHOD\nPATH\nBODY_SHA256\nTIMESTAMP\nNONCE
   *
   * @param contentSha256 when non-null, used directly as the body hash (for binary uploads where
   *     the client sends a Content-SHA256 header). When null, the body is hashed with SHA-256.
   */
  String computeSignature(
      String method,
      String path,
      String body,
      String timestamp,
      String nonce,
      String contentSha256) {
    try {
      // Use provided Content-SHA256 directly, or compute from body
      String bodyHash;
      if (contentSha256 != null && !contentSha256.isBlank()) {
        bodyHash = contentSha256;
      } else {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] bodyBytes = (body != null ? body : "").getBytes(StandardCharsets.UTF_8);
        bodyHash = bytesToHex(sha256.digest(bodyBytes));
      }

      // Build canonical request string
      String canonical =
          method.toUpperCase()
              + "\n"
              + path
              + "\n"
              + bodyHash
              + "\n"
              + timestamp
              + "\n"
              + (nonce != null ? nonce : "");

      // Compute HMAC
      Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(sharedSecret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
      byte[] sig = mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8));
      return bytesToHex(sig);
    } catch (Exception e) {
      throw new SecurityException("Failed to compute HMAC signature", e);
    }
  }

  /** Convenience overload for JSON requests (no Content-SHA256 header). */
  String computeSignature(String method, String path, String body, String timestamp, String nonce) {
    return computeSignature(method, path, body, timestamp, nonce, null);
  }

  /** Evicts nonces whose timestamps have fallen outside the drift window. */
  private void evictExpiredNonces(long cutoffEpochSeconds) {
    Iterator<Map.Entry<String, Long>> it = seenNonces.entrySet().iterator();
    while (it.hasNext()) {
      if (it.next().getValue() < cutoffEpochSeconds) {
        it.remove();
      }
    }
  }

  private void evictLruIfNeeded() {
    while (seenNonces.size() > maxNonceCacheEntries) {
      Iterator<Map.Entry<String, Long>> it = seenNonces.entrySet().iterator();
      if (!it.hasNext()) {
        break;
      }
      it.next();
      it.remove();
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
