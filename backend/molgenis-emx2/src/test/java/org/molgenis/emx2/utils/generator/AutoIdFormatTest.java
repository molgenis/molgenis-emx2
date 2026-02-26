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
      AutoIdFormat actual = AutoIdFormat.fromComputedString(computed);
      AutoIdFormat expected = new AutoIdFormat(format, length);
      assertEquals(expected, actual);
    }
  }

  @Test
  void givenFormat_thenCalculateNrValues() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.NUMBERS, 5);
    assertEquals(99_999, format.getMaxValue());
  }

  @Test
  void givenFormat_whenMaxValueExceedsLongSize_thenMaxValueIsMaxLongSize() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.MIXED, 11);
    assertEquals(Long.MAX_VALUE, format.getMaxValue());
  }

  @Test
  void givenValue_thenMapToFormat() {
    List<String> expected =
        IntStream.range(0, 10000)
            .boxed()
            .map(Object::toString)
            .map(str -> StringUtils.leftPad(str, 4, '0'))
            .map(str -> "FOO-" + str + "-BAR")
            .toList();

    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.NUMBERS, 4, "FOO-", "-BAR");
    List<String> actual = IntStream.range(0, 10000).boxed().map(format::mapToFormat).toList();

    assertEquals(expected, actual);
  }

  @Test
  void givenValue_thenReverse() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.MIXED, 4, "FOO-", "-BAR");
    assertEquals(1337, format.getValue("FOO-AAVj-BAR"));
    assertEquals(14_776_335, format.getValue("FOO-9999-BAR"));
    assertEquals(1, format.getValue("FOO-AAAB-BAR"));
    assertEquals(0, format.getValue("FOO-AAAA-BAR"));
  }

  @Test
  void givenValue_whenOutOfRange_thenThrow() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.MIXED, 4);
    assertThrows(MolgenisException.class, () -> format.getValue("AAAAA"));
  }

  @Test
  void givenValue_whenDifferentCharacters_thenThrow() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.NUMBERS, 4);
    assertThrows(MolgenisException.class, () -> format.getValue("A"));
  }

  @Test
  void givenFormat_thenValueComplies() {
    AutoIdFormat format = new AutoIdFormat(AutoIdFormat.Format.NUMBERS, 4, "FOO-", "-BAR");
    assertFalse(format.valueCompliesToFormat("FOO-123-BAR"));
    assertFalse(format.valueCompliesToFormat("FOO-1234"));
    assertFalse(format.valueCompliesToFormat("1234-BAR"));
    assertTrue(format.valueCompliesToFormat("FOO-1234-BAR"));
  }
}
