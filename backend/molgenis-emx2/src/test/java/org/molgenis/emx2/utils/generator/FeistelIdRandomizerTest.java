package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeistelIdRandomizerTest {

  @ParameterizedTest
  @ValueSource(longs = {10, 100, 1000, 1024, 10000, 100_000})
  void givenCompleteSet_whenRandomized_thenResultIsShuffledSet(long domain) {
    List<Long> original = LongStream.range(0, domain).boxed().toList();
    FeistelIdRandomizer randomizer = new FeistelIdRandomizer(domain);

    List<Long> randomized = original.stream().map(randomizer::randomize).toList();
    assertNotEquals(original, randomized);

    List<Long> sorted = randomized.stream().sorted().toList();
    assertEquals(original, sorted);
  }

  @Test
  void givenValue_whenRandomizedMultipleTimes_thenResultIsSame() {
    FeistelIdRandomizer randomizer = new FeistelIdRandomizer(1000);

    Set<Long> results = new HashSet<>();
    for (int i = 0; i < 100; i++) {
      results.add(randomizer.randomize(42));
    }

    assertEquals(1, results.size());
    assertFalse(results.contains(42L));
  }

  @Test
  void givenAutoIdFormatDomain_whenRandomized_thenResultIsUnique() {
    // 4-digit numeric: 10^4 = 10000
    long numericDomain = 10_000;
    FeistelIdRandomizer randomizer = new FeistelIdRandomizer(numericDomain);

    Set<Long> seen = new HashSet<>();
    for (long i = 0; i < numericDomain; i++) {
      long result = randomizer.randomize(i);
      assertTrue(result >= 0 && result < numericDomain, "Result out of range: " + result);
      assertTrue(seen.add(result), "Duplicate result: " + result);
    }
  }

  @Test
  void givenLettersDomain_whenRandomized_thenCoversFullRange() {
    // 2-char letters: 52^2 = 2704
    long lettersDomain = 52L * 52;
    FeistelIdRandomizer randomizer = new FeistelIdRandomizer(lettersDomain);

    Set<Long> seen = new HashSet<>();
    for (long i = 0; i < lettersDomain; i++) {
      long result = randomizer.randomize(i);
      assertTrue(result >= 0 && result < lettersDomain);
      assertTrue(seen.add(result));
    }
    assertEquals(lettersDomain, seen.size());
  }

  @Test
  void givenSmallDomain_thenStillProducesBijection() {
    FeistelIdRandomizer randomizer = new FeistelIdRandomizer(2);

    Set<Long> results = new HashSet<>();
    results.add(randomizer.randomize(0));
    results.add(randomizer.randomize(1));

    assertEquals(2, results.size());
    assertTrue(results.stream().allMatch(r -> r >= 0 && r < 2));
  }

  @ParameterizedTest
  @ValueSource(longs = {10, 100, 1000, 1024, 10000, 100_000})
  void givenRandomizedValue_whenReversed_thenReturnsOriginal(long domain) {
    FeistelIdRandomizer randomizer = new FeistelIdRandomizer(domain);

    for (long i = 0; i < domain; i++) {
      long randomized = randomizer.randomize(i);
      long reversed = randomizer.reverse(randomized);
      assertEquals(i, reversed, "reverse(randomize(" + i + ")) should equal " + i);
    }
  }

  @Test
  void givenDomainLessThanTwo_thenThrows() {
    assertThrows(IllegalArgumentException.class, () -> new FeistelIdRandomizer(1));
    assertThrows(IllegalArgumentException.class, () -> new FeistelIdRandomizer(0));
    assertThrows(IllegalArgumentException.class, () -> new FeistelIdRandomizer(-1));
  }
}
