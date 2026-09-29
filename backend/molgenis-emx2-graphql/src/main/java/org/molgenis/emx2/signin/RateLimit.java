package org.molgenis.emx2.signin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.*;
import java.util.Optional;

public class RateLimit {

  private static final Duration BASE_DURATION = Duration.ofSeconds(1);
  private static final Duration MAX_DURATION = Duration.ofHours(1);

  private static final Duration FORGET_AFTER = MAX_DURATION.multipliedBy(2);

  private static final long MAX_TRACKED_USERS = 100_000;

  private static RateLimit instance = null;

  private final InstantSource instantSource;
  private final Cache<String, Failures> failures =
      Caffeine.newBuilder().maximumSize(MAX_TRACKED_USERS).expireAfterWrite(FORGET_AFTER).build();

  public static synchronized RateLimit getInstance() {
    if (instance == null) {
      instance = new RateLimit(Clock.systemUTC());
    }
    return instance;
  }

  RateLimit(InstantSource instantSource) {
    this.instantSource = instantSource;
  }

  public synchronized void onFailure(String username) {
    Failures previous = failures.getIfPresent(username);

    int count = (previous == null ? 1 : previous.count() + 1);
    Instant blockedUntil = instantSource.instant().plus(delayFor(count));
    failures.put(username, new Failures(count, blockedUntil));
  }

  public synchronized void onSuccess(String username) {
    failures.invalidate(username);
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
}
