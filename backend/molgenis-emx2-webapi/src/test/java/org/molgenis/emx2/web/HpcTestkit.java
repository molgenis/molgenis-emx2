package org.molgenis.emx2.web;

import static io.restassured.RestAssured.given;

import io.restassured.specification.RequestSpecification;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

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
}
