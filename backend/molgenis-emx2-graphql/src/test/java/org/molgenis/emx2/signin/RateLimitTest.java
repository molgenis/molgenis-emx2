package org.molgenis.emx2.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.signin.RateLimit.AttemptResults;

class RateLimitTest {

  private static final String USER = "user";
  private static final String OTHER_USER = "other";

  private Instant now;
  private PrometheusRegistry registry;
  private RateLimit rateLimit;

  @BeforeEach
  void setUp() {
    now = Instant.parse("2026-01-01T00:00:00Z");
    registry = new PrometheusRegistry();
    rateLimit = new RateLimit(() -> now, registry);
  }

  @Test
  void givenNoFailures_thenNotRateLimited() {
    assertTrue(rateLimit.getRateLimit(USER).isEmpty());
  }

  @Test
  void givenOneFailure_thenLimitedForOneSecond() {
    rateLimit.onFailure(USER);

    assertEquals(Optional.of(now.plusSeconds(1)), rateLimit.getRateLimit(USER));
  }

  @Test
  void givenConsecutiveFailures_thenDelayDoubles() {
    long expectedSeconds = 1;
    for (int i = 0; i < 5; i++) {
      rateLimit.onFailure(USER);
      assertEquals(Optional.of(now.plusSeconds(expectedSeconds)), rateLimit.getRateLimit(USER));
      expectedSeconds *= 2;
    }
  }

  @Test
  void givenManyFailures_thenDelayIsCappedAtOneHour() {
    for (int i = 0; i < 100; i++) {
      rateLimit.onFailure(USER);
    }

    assertEquals(Optional.of(now.plus(Duration.ofHours(1))), rateLimit.getRateLimit(USER));
  }

  @Test
  void givenLimitExpired_thenNotRateLimited() {
    rateLimit.onFailure(USER);
    now = now.plusSeconds(1);

    assertTrue(rateLimit.getRateLimit(USER).isEmpty());
  }

  @Test
  void givenLimitExpired_whenFailingAgain_thenDelayKeepsGrowing() {
    rateLimit.onFailure(USER);
    now = now.plusSeconds(1);
    rateLimit.onFailure(USER);

    assertEquals(Optional.of(now.plusSeconds(2)), rateLimit.getRateLimit(USER));
  }

  @Test
  void givenFailures_whenSuccess_thenLimitIsReset() {
    rateLimit.onFailure(USER);
    rateLimit.onSuccess(USER);

    assertTrue(rateLimit.getRateLimit(USER).isEmpty());

    rateLimit.onFailure(USER);
    assertEquals(Optional.of(now.plusSeconds(1)), rateLimit.getRateLimit(USER));
  }

  @Test
  void givenFailuresForOneUser_thenOtherUserNotRateLimited() {
    rateLimit.onFailure(USER);

    assertTrue(rateLimit.getRateLimit(OTHER_USER).isEmpty());
  }

  @Test
  void givenNoAttempts_thenAllResultsReportedAsZero() {
    assertEquals(0, count(AttemptResults.SUCCESS.name()));
    assertEquals(0, count(AttemptResults.FAILURE.name()));
    assertEquals(0, count(AttemptResults.BLOCKED.name()));
  }

  @Test
  void givenFailures_thenFailuresCounted() {
    rateLimit.onFailure(USER);
    rateLimit.onFailure(OTHER_USER);

    assertEquals(2, count(AttemptResults.FAILURE.name()));
  }

  @Test
  void givenSuccess_thenSuccessCounted() {
    rateLimit.onSuccess(USER);

    assertEquals(1, count(AttemptResults.SUCCESS.name()));
  }

  @Test
  void givenBlocked_thenBlockedCounted() {
    rateLimit.onBlocked();

    assertEquals(1, count(AttemptResults.BLOCKED.name()));
  }

  @Test
  void givenManyUsers_thenOnlyOneSeriesPerResult() {
    for (int i = 0; i < 100; i++) {
      rateLimit.onFailure("user" + i);
    }

    assertEquals(3, signinAttempts().getDataPoints().size());
  }

  private CounterSnapshot signinAttempts() {
    return (CounterSnapshot)
        registry.scrape().stream()
            .filter(metric -> metric.getMetadata().getName().equals("emx2_signin_attempts"))
            .findFirst()
            .orElseThrow();
  }

  private double count(String result) {
    return signinAttempts().getDataPoints().stream()
        .filter(dataPoint -> result.equals(dataPoint.getLabels().get("result")))
        .findFirst()
        .orElseThrow()
        .getValue();
  }
}
