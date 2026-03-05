package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.MolgenisException;

class AutoIdFormatTest {

  @Nested
  class ParseInputTest {

    @Test
    void givenAutoIdToken_thenUseDefaultValues() {
      assertTrue(AutoIdFormat.fromComputedString("${mg_autoid}").isEmpty());
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
          MolgenisException.class,
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
          MolgenisException.class,
          () -> AutoIdFormat.fromComputedString("${mg_autoid(unknown=bar)}"));
    }

    @Test
    void givenKeyTwice_thenThrowException() {
      assertThrows(
          MolgenisException.class,
          () -> AutoIdFormat.fromComputedString("${mg_autoid(length=42, length=42)}"));
    }

    @Test
    void givenAutoIdTwice_thenThrowException() {
      String input =
          "FOO-${mg_autoid(length=4, format=numbers)}-${mg_autoid(length=6, format=numbers)}";
      assertThrows(MolgenisException.class, () -> AutoIdFormat.fromComputedString(input));
    }

    @Test
    void shouldNormalizeArguments() {
      assertComputedTransformsToConfig("${mg_autoid(LENGTH=42)}", AutoIdFormat.Format.MIXED, 42);
      assertComputedTransformsToConfig("${mg_autoid(LeNgTh=42)}", AutoIdFormat.Format.MIXED, 42);
    }

    void assertComputedTransformsToConfig(String computed, AutoIdFormat.Format format, int length) {
      AutoIdFormat actual =
          AutoIdFormat.fromComputedString(computed)
              .orElseThrow(() -> new AssertionError("Expected AutoIdFormat"));
      AutoIdFormat expected = new AutoIdFormat(format, length);
      assertEquals(expected, actual);
    }
  }
}
