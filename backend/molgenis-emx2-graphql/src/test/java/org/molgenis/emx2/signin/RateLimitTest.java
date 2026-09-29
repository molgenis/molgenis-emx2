package org.molgenis.emx2.signin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateLimitTest {

  private static final String USER = "user";
  private static final String OTHER_USER = "other";

  private Instant now;
  private RateLimit rateLimit;

  @BeforeEach
  void setUp() {
    now = Instant.parse("2026-01-01T00:00:00Z");
    rateLimit = new RateLimit(() -> now);
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
}
