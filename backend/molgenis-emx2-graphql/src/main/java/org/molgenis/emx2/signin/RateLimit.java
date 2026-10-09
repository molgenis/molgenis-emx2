package org.molgenis.emx2.signin;

import static org.molgenis.emx2.signin.RateLimit.AttemptResults.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import java.time.*;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RateLimit {

  private static final Logger logger = LoggerFactory.getLogger(RateLimit.class);

  private static final Duration BASE_DURATION = Duration.ofSeconds(1);
  private static final Duration MAX_DURATION = Duration.ofHours(1);
  private static final Duration FORGET_AFTER = MAX_DURATION.multipliedBy(2);
  private static final long MAX_TRACKED_USERS = 100_000;

  private static RateLimit instance = null;

  private final InstantSource instantSource;
  private final Counter attempts;

  private final Cache<String, Failures> failures =
      Caffeine.newBuilder().maximumSize(MAX_TRACKED_USERS).expireAfterWrite(FORGET_AFTER).build();

  public static synchronized RateLimit getInstance() {
    if (instance == null) {
      instance = new RateLimit(Clock.systemUTC(), PrometheusRegistry.defaultRegistry);
    }
    return instance;
  }

  RateLimit(InstantSource instantSource, PrometheusRegistry registry) {
    this.instantSource = instantSource;
    this.attempts =
        Counter.builder()
            .name("emx2_signin_attempts_total")
            .help("Sign-in attempts by result: success, failure or blocked by the rate limit")
            .labelNames("result")
            .register(registry);

    // create the series up front so they are reported as 0 before the first attempt
    Stream.of(SUCCESS, FAILURE, BLOCKED).map(Enum::name).forEach(attempts::labelValues);
  }

  public synchronized void onFailure(String username) {
    attempts.labelValues(FAILURE.name()).inc();
    Failures previous = failures.getIfPresent(username);
    int count = (previous == null ? 1 : previous.count() + 1);
    Instant blockedUntil = instantSource.instant().plus(delayFor(count));
    failures.put(username, new Failures(count, blockedUntil));
    logger.warn(
        "Sign in as '{}' failed ({} consecutive failures), blocked until {}",
        username,
        count,
        blockedUntil);
  }

  public synchronized void onSuccess(String username) {
    attempts.labelValues(SUCCESS.name()).inc();
    failures.invalidate(username);
  }

  public void onBlocked() {
    attempts.labelValues(BLOCKED.name()).inc();
  }

  public synchronized Optional<Instant> getRateLimit(String username) {
    Failures current = failures.getIfPresent(username);
    if (current == null || !current.blockedUntil().isAfter(instantSource.instant())) {
      return Optional.empty();
    }
    return Optional.of(current.blockedUntil());
  }

  private static Duration delayFor(int count) {
    int exponent = Math.min(count - 1, 30);
    Duration delay = BASE_DURATION.multipliedBy(1L << exponent);
    return delay.compareTo(MAX_DURATION) > 0 ? MAX_DURATION : delay;
  }

  private record Failures(int count, Instant blockedUntil) {}

  enum AttemptResults {
    SUCCESS,
    FAILURE,
    BLOCKED
  }
}
