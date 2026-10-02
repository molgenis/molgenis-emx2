package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.assertInvalid;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.parse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResolveStaticFieldSpecTest {

  @Test
  void givenAllArguments_whenParsed_thenMapEachArgument() {
    PostProcessorSpec spec =
        parse(
            """
            resolve-static:
              table: Collections
              field: type
              value: http://semanticscience.org/resource/SIO_001067
            """);

    assertEquals(
        new ResolveStaticFieldSpec(
            "Collections", "type", "http://semanticscience.org/resource/SIO_001067"),
        spec);
  }

  @Test
  void givenNumericValue_whenParsed_thenKeepNumber() {
    PostProcessorSpec spec = parse("resolve-static: { table: Collections, field: size, value: 3 }");

    assertEquals(new ResolveStaticFieldSpec("Collections", "size", 3), spec);
  }

  @Test
  void givenUnknownArgument_whenParsed_thenThrowException() {
    assertInvalid(
        "resolve-static: { table: Collections, field: type, value: x, extra: y }", "extra");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "resolve-static: { field: type, value: x }",
        "resolve-static: { table: Collections, value: x }",
        "resolve-static: { table: Collections, field: type }",
        "resolve-static: {}",
        "resolve-static"
      })
  void givenMissingRequiredArgument_whenParsed_thenThrowException(String yaml) {
    assertInvalid(yaml, "resolve-static requires table, field and value");
  }
}
