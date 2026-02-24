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
        () -> verifier.verify("POST", "/api/hpc/jobs", "{}", authHeader, timestamp, nonce));
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
                "POST", "/api/hpc/jobs", "{}", "HMAC-SHA256 invalidsignature", timestamp, nonce));
  }

  @Test
  void missingAuthHeaderThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

    assertThrows(
        SecurityException.class,
        () -> verifier.verify("GET", "/api/hpc/jobs", "", null, timestamp, "nonce"));
  }

  @Test
  void wrongAuthPrefixThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

    assertThrows(
        SecurityException.class,
        () -> verifier.verify("GET", "/api/hpc/jobs", "", "Bearer token123", timestamp, "nonce-2"));
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
        () -> verifier.verify("GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig, timestamp, nonce));
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
        () -> verifier.verify("GET", "/api/hpc/jobs", "", authHeader, timestamp, nonce));

    // Replayed request fails
    assertThrows(
        SecurityException.class,
        () -> verifier.verify("GET", "/api/hpc/jobs", "", authHeader, timestamp, nonce));
  }

  @Test
  void missingNonceThrows() {
    HmacVerifier verifier = new HmacVerifier(SECRET);
    String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
    String sig = verifier.computeSignature("GET", "/api/hpc/jobs", "", timestamp, "");

    assertThrows(
        SecurityException.class,
        () -> verifier.verify("GET", "/api/hpc/jobs", "", "HMAC-SHA256 " + sig, timestamp, null));
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
}
