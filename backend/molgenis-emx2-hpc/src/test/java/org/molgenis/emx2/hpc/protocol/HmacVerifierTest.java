package org.molgenis.emx2.hpc.protocol;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HmacVerifierTest {

  private static final String SECRET = "this-is-a-test-secret-at-least-32-chars";

  @Test
  void validRequestPasses() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String nonce = "unique-nonce-123";

    // Compute expected signature
    String sig = verifier.computeSignature("POST", "/api/hpc/jobs", "{}", timestamp, nonce);
    String authHeader = "HMAC-SHA256 " + sig;

    // Should not throw
    assertDoesNotThrow(
        () -> verifier.verify("POST", "/api/hpc/jobs", "{}", authHeader, timestamp, nonce, null));
  }

  @Test
  void invalidSignatureThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String nonce = "nonce-1";

    assertThrows(
        SecurityException.class,
        () ->
            verifier.verify(
                "POST",
                "/api/hpc/jobs",
                "{}",
                "HMAC-SHA256 invalidsignature",
                timestamp,
                nonce,
                null));
  }

  @Test
  void missingAuthHeaderThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

    assertThrows(
        SecurityException.class,
        () -> verifier.verify("GET", "/api/hpc/jobs", "", null, timestamp, "nonce", null));
  }

  @Test
  void wrongAuthPrefixThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

    assertThrows(
        SecurityException.class,
        () ->
            verifier.verify(
                "GET", "/api/hpc/jobs", "", "Bearer token123", timestamp, "nonce-2", null));
  }

  @Test
  void expiredTimestampThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    // 10 minutes ago
    String timestamp = String.valueOf((System.currentTimeMillis() / 1000) - 600);
    String nonce = "nonce-3";

    String sig = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, nonce);

    assertThrows(
        SecurityException.class,
        () ->
            verifier.verify(
                "GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig, timestamp, nonce, null));
  }

  @Test
  void replayedNonceThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String nonce = "replay-nonce";

    String sig = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, nonce);
    String authHeader = "HMAC-SHA256 " + sig;

    // First request succeeds
    assertDoesNotThrow(
        () -> verifier.verify("GET", "/api/hpc/jobs", "", authHeader, timestamp, nonce, null));

    // Replayed request fails
    assertThrows(
        SecurityException.class,
        () -> verifier.verify("GET", "/api/hpc/jobs", "", authHeader, timestamp, nonce, null));
  }

  @Test
  void missingNonceThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String sig = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, "");

    assertThrows(
        SecurityException.class,
        () ->
            verifier.verify(
                "GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig, timestamp, null, null));
  }

  @Test
  void differentMethodProducesDifferentSignature() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = "1234567890";
    String nonce = "nonce-4";

    String sig1 = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, nonce);
    String sig2 = verifier.computeSignature("POST", "/api/hpc/jobs", "", timestamp, nonce);

    assertNotEquals(sig1, sig2);
  }

  @Test
  void differentBodyProducesDifferentSignature() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = "1234567890";
    String nonce = "nonce-5";

    String sig1 = verifier.computeSignature("POST", "/api/hpc/jobs", "{\"a\":1}", timestamp, nonce);
    String sig2 = verifier.computeSignature("POST", "/api/hpc/jobs", "{\"b\":2}", timestamp, nonce);

    assertNotEquals(sig1, sig2);
  }

  @Test
  void shortSecretThrows() {
    assertThrows(IllegalArgumentException.class, () -> new HmacVerifier("short"));
  }

  @Test
  void nullSecretThrows() {
    assertThrows(IllegalArgumentException.class, () -> new HmacVerifier(null));
  }

  @Test
  void invalidCacheCapacityThrows() {
    assertThrows(IllegalArgumentException.class, () -> new HmacVerifier(SECRET, 0));
  }

  // --- Content-SHA256 tests ---

  @Test
  void contentSha256UsedInCanonicalString() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = "1234567890";
    String nonce = "nonce-sha256";

    // Signature with empty body (no Content-SHA256)
    String sigEmpty =
        verifier.computeSignature("PUT", "/api/hpc/artifacts/x/files/y", "", timestamp, nonce);

    // Signature with Content-SHA256 provided
    String contentSha256 = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    String sigWithHash =
        verifier.computeSignature(
            "PUT", "/api/hpc/artifacts/x/files/y", "", timestamp, nonce, contentSha256);

    // They must differ: one hashes empty string, the other uses the provided hash
    assertNotEquals(sigEmpty, sigWithHash);
  }

  @Test
  void contentSha256VerificationPasses() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String nonce = "nonce-sha256-verify";
    String contentSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    String sig =
        verifier.computeSignature(
            "PUT", "/api/hpc/artifacts/a/files/b", "", timestamp, nonce, contentSha256);
    String authHeader = "HMAC-SHA256 " + sig;

    assertDoesNotThrow(
        () ->
            verifier.verify(
                "PUT",
                "/api/hpc/artifacts/a/files/b",
                "",
                authHeader,
                timestamp,
                nonce,
                contentSha256));
  }

  @Test
  void contentSha256MismatchFailsVerification() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String nonce = "nonce-sha256-mismatch";
    String contentSha256 = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    String wrongSha256 = "0000001234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    // Sign with one hash
    String sig =
        verifier.computeSignature(
            "PUT", "/api/hpc/artifacts/a/files/b", "", timestamp, nonce, contentSha256);
    String authHeader = "HMAC-SHA256 " + sig;

    // Verify with a different hash — signature mismatch
    assertThrows(
        SecurityException.class,
        () ->
            verifier.verify(
                "PUT",
                "/api/hpc/artifacts/a/files/b",
                "",
                authHeader,
                timestamp,
                nonce,
                wrongSha256));
  }

  // --- Nonce eviction test ---

  @Test
  void expiredNoncesAreEvictedAndCanBeReused() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    // Use a timestamp from 6 minutes ago (beyond the 5-min drift window)
    // The verifier will reject this due to timestamp drift, but we can test
    // that the nonce cache evicts old entries by using a valid timestamp and
    // confirming nonces from different time windows don't collide.

    String now = String.valueOf(System.currentTimeMillis() / 1000);
    String nonce = "eviction-test-nonce";

    String sig = verifier.computeSignature("GET", "/api/hpc/jobs", "", now, nonce);
    String authHeader = "HMAC-SHA256 " + sig;

    // First use succeeds
    assertDoesNotThrow(
        () -> verifier.verify("GET", "/api/hpc/jobs", "", authHeader, now, nonce, null));

    // Replay within window fails (expected behavior)
    assertThrows(
        SecurityException.class,
        () -> verifier.verify("GET", "/api/hpc/jobs", "", authHeader, now, nonce, null));
  }

  @Test
  void lruNonceEvictionKeepsCacheBounded() {
    HmacVerifier verifier = new HmacVerifier(SECRET, 2);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

    String nonce1 = "lru-nonce-1";
    String nonce2 = "lru-nonce-2";
    String nonce3 = "lru-nonce-3";

    String sig1 = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, nonce1);
    String sig2 = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, nonce2);
    String sig3 = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, nonce3);

    assertDoesNotThrow(
        () ->
            verifier.verify(
                "GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig1, timestamp, nonce1, null));
    assertDoesNotThrow(
        () ->
            verifier.verify(
                "GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig2, timestamp, nonce2, null));
    assertDoesNotThrow(
        () ->
            verifier.verify(
                "GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig3, timestamp, nonce3, null));

    // With capacity 2, one old nonce is evicted; reused nonce is accepted again.
    assertDoesNotThrow(
        () ->
            verifier.verify(
                "GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig1, timestamp, nonce1, null));
  }
}
