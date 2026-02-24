package org.molgenis.emx2.hpc.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 * <p>Also provides nonce replay protection and timestamp drift validation.
 */
public final class HmacVerifier {

  private static final Logger logger = LoggerFactory.getLogger(HmacVerifier.class);
  private static final String ALGORITHM = "HmacSHA256";
  private static final long MAX_TIMESTAMP_DRIFT_SECONDS = 300; // 5 minutes
  private static final int MAX_NONCE_CACHE_SIZE = 10_000;

  private final String sharedSecret;

  /**
   * LRU cache for nonces seen within the timestamp window. Evicts oldest entries when full. Access
   * is synchronized since the before-handler may run concurrently.
   */
  private final Map<String, Long> seenNonces =
      new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
          return size() > MAX_NONCE_CACHE_SIZE;
        }
      };

  public HmacVerifier(String sharedSecret) {
    if (sharedSecret == null || sharedSecret.length() < 32) {
      throw new IllegalArgumentException(
          "Shared secret must be at least 32 characters for adequate security");
    }
    this.sharedSecret = sharedSecret;
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
   * @throws SecurityException if verification fails
   */
  public void verify(
      String method, String path, String body, String authHeader, String timestamp, String nonce) {
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

    // 3. Nonce replay check
    if (nonce == null || nonce.isBlank()) {
      throw new SecurityException("Missing X-Nonce header");
    }
    synchronized (seenNonces) {
      if (seenNonces.containsKey(nonce)) {
        throw new SecurityException("Duplicate nonce (possible replay attack)");
      }
      seenNonces.put(nonce, requestTime);
      evictExpiredNonces();
    }

    // 4. Compute and verify signature
    String expectedSignature = computeSignature(method, path, body, timestamp, nonce);
    if (!MessageDigest.isEqual(
        expectedSignature.getBytes(StandardCharsets.UTF_8),
        providedSignature.getBytes(StandardCharsets.UTF_8))) {
      throw new SecurityException("HMAC signature mismatch");
    }
  }

  /**
   * Computes the HMAC-SHA256 signature for a request using the same canonical format as the Python
   * client: METHOD\nPATH\nBODY_SHA256\nTIMESTAMP\nNONCE
   */
  String computeSignature(String method, String path, String body, String timestamp, String nonce) {
    try {
      // Hash the body with SHA-256
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] bodyBytes = (body != null ? body : "").getBytes(StandardCharsets.UTF_8);
      String bodyHash = bytesToHex(sha256.digest(bodyBytes));

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

  /** Evicts nonces that are beyond the timestamp drift window. Must be called under lock. */
  private void evictExpiredNonces() {
    long cutoff = (System.currentTimeMillis() / 1000) - MAX_TIMESTAMP_DRIFT_SECONDS;
    seenNonces.entrySet().removeIf(e -> e.getValue() < cutoff);
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
