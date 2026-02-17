package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class LongPackTest {

  @Test
  void shouldGetMaxPackValue() {
    long maxValue = LongPack.maxPackValue(List.of(8L, 10L, 4L));
    assertEquals(494, maxValue);
  }

  @Test
  void inputShouldNotSurpassMaxValues() {
    List<Long> numbers = List.of(1L, 4L);
    List<Long> maxValues = List.of(1L, 3L);
    assertThrows(
        IllegalArgumentException.class,
        () -> new LongPack(numbers, maxValues),
        "Number 4 is greater than max value 3");
  }

  @Test
  void shouldCapAtMaxLongValue() {
    long maxValue = LongPack.maxPackValue(List.of(8L, Long.MAX_VALUE, 4L));
    assertEquals(Long.MAX_VALUE, maxValue);
  }

  @Test
  void shouldHandleInvalidInput() {
    List<Long> numbers = List.of(1L, 2L);
    List<Long> maxValues = List.of(1L);
    assertThrows(IllegalArgumentException.class, () -> new LongPack(numbers, maxValues));
  }

  @Test
  void shouldMapCompleteSetUniquely() {
    long max1 = 7;
    long max2 = 9;
    long max3 = 3;
    List<List<Long>> triplets = new ArrayList<>();

    for (long i = 0; i <= max1; i++) {
      for (long j = 0; j <= max2; j++) {
        for (long k = 0; k <= max3; k++) {
          triplets.add(List.of(i, j, k));
        }
      }
    }

    List<List<Long>> result =
        LongStream.range(0, LongPack.maxPackValue(List.of(max1, max2, max3)) + 1)
            .boxed()
            .map(x -> LongPack.fromValue(x, List.of(max1, max2, max3)))
            .map(LongPack::numbers)
            .toList();

    assertEquals(triplets.size(), result.size());
    for (int i = 0; i < triplets.size(); i++) {
      try {

        assertEquals(triplets.get(i), result.get(i));
      } catch (IndexOutOfBoundsException e) {
        System.out.println("test");
      }
    }
  }
}
