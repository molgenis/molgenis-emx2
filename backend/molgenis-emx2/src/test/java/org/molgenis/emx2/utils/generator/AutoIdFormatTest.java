package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AutoIdFormatTest {

  @Nested
  class ParseInputTest {

    @Test
    void givenAutoIdToken_thenUseDefaultValues() {
      assertComputedTransformsToConfig("${mg_autoid}", AutoIdFormat.Format.MIXED, 12);
    }

    @Test
    void givenAutoIdWithLength_thenConfigureLength() {
      assertComputedTransformsToConfig("${mg_autoid(length=42)}", AutoIdFormat.Format.MIXED, 42);
      assertComputedTransformsToConfig(
          "${mg_autoid(length=1337)}", AutoIdFormat.Format.MIXED, 1337);
    }

    @Test
    void givenAutoIdWithFormats_thenConfigureFormats() {
      assertComputedTransformsToConfig(
          "${mg_autoid(format=LETTERS)}", AutoIdFormat.Format.LETTERS, 12);
      assertComputedTransformsToConfig(
          "${mg_autoid(format=numbers)}", AutoIdFormat.Format.NUMBERS, 12);
      assertComputedTransformsToConfig("${mg_autoid(format=mixed)}", AutoIdFormat.Format.MIXED, 12);
    }

    @Test
    void givenNegativeLength_thenThrowException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AutoIdFormat.fromComputedString("${mg_autoid(length=-1)}"));
    }

    @Test
    void givenNonNumericLength_thenThrow() {
      assertThrows(
          NumberFormatException.class,
          () -> AutoIdFormat.fromComputedString("${mg_autoid(length=foo)}"));
    }

    @Test
    void givenBothArguments_thenConfigureBoth() {
      assertComputedTransformsToConfig(
          "${mg_autoid(length=42, format=letters)}", AutoIdFormat.Format.LETTERS, 42);
      assertComputedTransformsToConfig(
          "${mg_autoid(format=numbers, length=1337)}", AutoIdFormat.Format.NUMBERS, 1337);
    }

    @Test
    void givenUnknownArgument_thenThrowException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AutoIdFormat.fromComputedString("${mg_autoid(unknown=bar)}"));
    }

    @Test
    void givenKeyTwice_thenThrowException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AutoIdFormat.fromComputedString("${mg_autoid(length=42, length=42)}"));
    }

    @Test
    void shouldNormalizeArguments() {
      assertComputedTransformsToConfig("${mg_autoid(LENGTH=42)}", AutoIdFormat.Format.MIXED, 42);
      assertComputedTransformsToConfig("${mg_autoid(LeNgTh=42)}", AutoIdFormat.Format.MIXED, 42);
    }

    void assertComputedTransformsToConfig(String computed, AutoIdFormat.Format format, int length) {
      AutoIdFormat actual = AutoIdFormat.fromComputedString(computed);
      AutoIdFormat expected = new AutoIdFormat(format, length);
      assertEquals(expected, actual);
    }
  }

  @Test
  void givenFormat_thenCalculateNrValues() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.NUMBERS, 5);
    assertEquals(100_000, format.getMaxValue());
  }

  @Test
  void givenFormat_whenMaxValueExceedsLongSize_thenMaxValueIsMaxLongSize() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.MIXED, 11);
    assertEquals(Long.MAX_VALUE, format.getMaxValue());
  }
}
