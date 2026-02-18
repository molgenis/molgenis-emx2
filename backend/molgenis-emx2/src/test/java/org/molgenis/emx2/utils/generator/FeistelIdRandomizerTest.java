package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class FeistelIdRandomizerTest {

  @Test
  void givenCompleteSet_whenRandomized_thenResultIsShuffledSet() {
    List<Long> original = LongStream.range(0, 1000).boxed().toList();
    FeistelIdRandomizer randomizer = new FeistelIdRandomizer(1000);

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
      results.add(randomizer.randomize(1337));
    }

    assertEquals(1, results.size());
    assertFalse(results.contains(1337L));
  }
}
