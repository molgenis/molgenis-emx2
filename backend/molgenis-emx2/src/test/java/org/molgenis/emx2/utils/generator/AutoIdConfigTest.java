package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AutoIdConfigTest {

  @Test
  void givenAutoIdToken_thenUseDefaultValues() {
    assertComputedTransformsToConfig("${mg_autoid}", AutoIdConfig.Format.MIXED, 12);
  }

  @Test
  void givenAutoIdWithLength_thenConfigureLength() {
    assertComputedTransformsToConfig("${mg_autoid(length=42)}", AutoIdConfig.Format.MIXED, 42);
    assertComputedTransformsToConfig("${mg_autoid(length=1337)}", AutoIdConfig.Format.MIXED, 1337);
  }

  @Test
  void givenAutoIdWithFormats_thenConfigureFormats() {
    assertComputedTransformsToConfig(
        "${mg_autoid(format=LETTERS)}", AutoIdConfig.Format.LETTERS, 12);
    assertComputedTransformsToConfig(
        "${mg_autoid(format=numbers)}", AutoIdConfig.Format.NUMBERS, 12);
    assertComputedTransformsToConfig("${mg_autoid(format=mixed)}", AutoIdConfig.Format.MIXED, 12);
  }

  @Test
  void givenNegativeLength_thenThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AutoIdConfig.fromComputedString("${mg_autoid(length=-1)}"));
  }

  @Test
  void givenNonNumericLength_thenThrow() {
    assertThrows(
        NumberFormatException.class,
        () -> AutoIdConfig.fromComputedString("${mg_autoid(length=foo)}"));
  }

  @Test
  void givenBothArguments_thenConfigureBoth() {
    assertComputedTransformsToConfig(
        "${mg_autoid(length=42, format=letters)}", AutoIdConfig.Format.LETTERS, 42);
    assertComputedTransformsToConfig(
        "${mg_autoid(format=numbers, length=1337)}", AutoIdConfig.Format.NUMBERS, 1337);
  }

  @Test
  void givenUnknownArgument_thenThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AutoIdConfig.fromComputedString("${mg_autoid(unknown=bar)}"));
  }

  @Test
  void givenKeyTwice_thenThrowException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> AutoIdConfig.fromComputedString("${mg_autoid(length=42, length=42)}"));
  }

  @Test
  void shouldNormalizeArguments() {
    assertComputedTransformsToConfig("${mg_autoid(LENGTH=42)}", AutoIdConfig.Format.MIXED, 42);
    assertComputedTransformsToConfig("${mg_autoid(LeNgTh=42)}", AutoIdConfig.Format.MIXED, 42);
  }

  void assertComputedTransformsToConfig(String computed, AutoIdConfig.Format format, int length) {
    AutoIdConfig actual = AutoIdConfig.fromComputedString(computed);
    AutoIdConfig expected = new AutoIdConfig(format, length);
    assertEquals(expected, actual);
  }
}
