package org.molgenis.emx2.sql.autoid;

import java.util.ArrayList;
import java.util.List;

public record LongPack(List<Long> numbers, List<Long> maxValues) {

  public LongPack(List<Long> numbers, List<Long> maxValues) {
    if (numbers.size() != maxValues.size()) {
      throw new IllegalArgumentException("Numbers and Max Values must have same size");
    }

    for (int i = 0; i < numbers.size(); i++) {
      if (numbers.get(i) > maxValues.get(i)) {
        throw new IllegalArgumentException(
            "Number " + numbers.get(i) + " is greater than max value " + maxValues.get(i));
      }
    }

    this.numbers = new ArrayList<>(numbers);
    this.maxValues = maxValues;
  }

  public static LongPack fromValue(long value, List<Long> maxValues) {
    List<Long> numbers = new ArrayList<>();
    long remaining = value;
    List<Long> bases = basesForMaxValues(maxValues);

    for (int i = 0; i < maxValues.size(); i++) {
      long base = bases.get(i);
      long number = remaining / base;
      numbers.add(number);
      remaining %= base;
    }

    return new LongPack(numbers, maxValues);
  }

  public static long maxPackValue(List<Long> maxValues) {
    long max = 0;
    long cumulativeBase = 1;

    for (Long maxValue : maxValues) {
      if (Long.MAX_VALUE / maxValue < max) {
        return Long.MAX_VALUE;
      }

      max += maxValue * cumulativeBase;
      cumulativeBase *= (maxValue + 1);
    }

    return max;
  }

  private static List<Long> basesForMaxValues(List<Long> maxValues) {
    List<Long> bases = new ArrayList<>();
    long cumulativeBase = 1;
    for (int i = maxValues.size() - 1; i >= 0; i--) {
      bases.addFirst(cumulativeBase);
      cumulativeBase *= (maxValues.get(i) + 1);
    }

    return bases;
  }
}
