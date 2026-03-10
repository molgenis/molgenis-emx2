package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Shared helper utilities for deterministic HPC API integration tests. */
final class HpcTestkit {

  private static final AtomicLong ID_SEQ = new AtomicLong(1);
  private static final AtomicLong TS_SEQ = new AtomicLong(0);
  private static final Instant BASE_TIMESTAMP = Instant.parse("2026-01-01T00:00:00Z");

  private HpcTestkit() {}

  static String nextUuid() {
    String token = "hpc-testkit-" + ID_SEQ.getAndIncrement();
    return UUID.nameUUIDFromBytes(token.getBytes(StandardCharsets.UTF_8)).toString();
  }

  static String nextName(String prefix) {
    return prefix + "-" + nextUuid();
  }

  static RequestSpecification hpcRequest(String sessionId) {
    RequestSpecification req =
        given()
            .header("X-EMX2-API-Version", "2025-01")
            .header("X-Request-Id", nextUuid())
            .header("X-Timestamp", BASE_TIMESTAMP.plusSeconds(TS_SEQ.getAndIncrement()).toString())
            .contentType("application/json");
    if (sessionId != null) {
      req = req.sessionId(sessionId);
    }
    return req;
  }

  static Map<String, String> hmacHeaders(
      String method, String pathWithQuery, String body, String sharedSecret) {
    String timestamp = String.valueOf(Instant.now().getEpochSecond());
    String nonce = nextUuid().replace("-", "");
    String requestId = nextUuid();
    String signature = signHmac(method, pathWithQuery, body, timestamp, nonce, sharedSecret);

    Map<String, String> headers = new LinkedHashMap<>();
    headers.put("Authorization", "HMAC-SHA256 " + signature);
    headers.put("X-EMX2-API-Version", "2025-01");
    headers.put("X-Request-Id", requestId);
    headers.put("X-Timestamp", timestamp);
    headers.put("X-Nonce", nonce);
    return headers;
  }

  private static String signHmac(
      String method,
      String pathWithQuery,
      String body,
      String timestamp,
      String nonce,
      String secret) {
    try {
      String canonicalBody = body != null ? body : "";
      byte[] bodyHash =
          java.security.MessageDigest.getInstance("SHA-256")
              .digest(canonicalBody.getBytes(StandardCharsets.UTF_8));
      String bodyHashHex = HexFormat.of().formatHex(bodyHash);
      String canonical =
          method.toUpperCase()
              + "\n"
              + pathWithQuery
              + "\n"
              + bodyHashHex
              + "\n"
              + timestamp
              + "\n"
              + nonce;

      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new RuntimeException("Failed to sign HMAC test request", e);
    }
  }
}
