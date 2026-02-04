package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class FormattedIdGeneratorTest {

  private static final String SNOWFLAKE_REGEX = "[a-zA-Z\\d]{10}";

  @BeforeAll
  static void setupSnowflake() {
    SnowflakeIdGenerator.init("123");
  }

  private static Stream<Arguments> nonArgumentTokens() {
    return Stream.of(
        Arguments.of("${mg_autoid}", SNOWFLAKE_REGEX),
        Arguments.of("foo${mg_autoid}bar", "foo" + SNOWFLAKE_REGEX + "bar"),
        Arguments.of(
            "foo${mg_autoid}bar${mg_autoid}", "foo" + SNOWFLAKE_REGEX + "bar" + SNOWFLAKE_REGEX));
  }

  @ParameterizedTest
  @MethodSource("nonArgumentTokens")
  void shouldHandleNoArgumentsInToken(String input, String expectedFormat) {
    assertInputTranslatesToGivenFormat(input, expectedFormat);
  }

  private static Stream<Arguments> argumentTokens() {
    return Stream.of(
        Arguments.of("${mg_autoid()}", "[a-zA-Z\\d]{12}"),
        Arguments.of("${mg_autoid(        )}", "[a-zA-Z\\d]{12}"),
        Arguments.of("${mg_autoid(length=1, format=numbers)}", "\\d"),
        Arguments.of("foo${mg_autoid(length=1, format=numbers)}bar", "foo\\dbar"),
        Arguments.of(
            "foo${mg_autoid(length=1, format=numbers)}bar${mg_autoid(length=1, format=letters)}",
            "foo\\dbar[a-zA-Z]"));
  }

  @ParameterizedTest
  @MethodSource("argumentTokens")
  void shouldHandleArgumentsInToken(String input, String expectedFormat) {
    assertInputTranslatesToGivenFormat(input, expectedFormat);
  }

  private void assertInputTranslatesToGivenFormat(String input, String expectedFormatRegex) {
    IdGenerator generator = FormattedIdGenerator.fromFormat(input);
    String id = generator.generateId();
    Matcher matcher = Pattern.compile(expectedFormatRegex).matcher(id);
    assertTrue(matcher.find());
  }
}
