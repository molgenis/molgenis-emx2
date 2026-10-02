package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.assertInvalid;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.parse;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CoalesceFieldSpecTest {

  @Test
  void givenAllArguments_whenParsed_thenMapEachArgument() {
    PostProcessorSpec spec =
        parse(
            """
            coalesce-field:
              table: Collections
              field: id
              derive-from: [acronym, name]
              strict: false
            """);

    assertEquals(
        new CoalesceFieldSpec("Collections", "id", List.of("acronym", "name"), false), spec);
  }

  @Test
  void givenNoStrict_whenParsed_thenDefaultToStrict() {
    PostProcessorSpec spec =
        parse(
            """
            coalesce-field:
              table: Collections
              field: id
              derive-from: [acronym]
            """);

    assertEquals(new CoalesceFieldSpec("Collections", "id", List.of("acronym"), true), spec);
  }

  @Test
  void givenFlowStyle_whenParsed_thenSameAsBlockStyle() {
    PostProcessorSpec spec =
        parse("coalesce-field: { table: Collections, field: id, derive-from: [acronym, name] }");

    assertEquals(
        new CoalesceFieldSpec("Collections", "id", List.of("acronym", "name"), true), spec);
  }

  @Test
  void givenDeriveFromAsBlockList_whenParsed_thenKeepListedOrder() {
    PostProcessorSpec spec =
        parse(
            """
            coalesce-field:
              table: Collections
              field: id
              derive-from:
                - name
                - acronym
            """);

    assertEquals(List.of("name", "acronym"), ((CoalesceFieldSpec) spec).deriveFrom());
  }

  @Test
  void givenSingleDeriveFromValue_whenParsed_thenThrowException() {
    assertInvalid(
        """
        coalesce-field: { table: Collections, field: id, derive-from: acronym }
        """,
        "derive-from");
  }

  @Test
  void givenCamelCaseArgument_whenParsed_thenRejectAsUnknown() {
    assertInvalid(
        """
        coalesce-field: { table: Collections, field: id, deriveFrom: [acronym] }
        """,
        "deriveFrom");
  }

  @Test
  void givenUnknownArgument_whenParsed_thenThrowException() {
    assertInvalid(
        """
        coalesce-field: { table: Collections, field: id, derive-from: [acronym], extra: x }
        """,
        "extra");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "coalesce-field: { field: id, derive-from: [acronym] }",
        "coalesce-field: { table: Collections, derive-from: [acronym] }",
        "coalesce-field: { table: Collections, field: id }",
        "coalesce-field: { table: Collections, field: id, derive-from: [] }",
        "coalesce-field: {}"
      })
  void givenMissingRequiredArgument_whenParsed_thenThrowException(String yaml) {
    assertInvalid(yaml, "coalesce-field requires table, field and derive-from");
  }

  @Test
  void givenNonBooleanStrict_whenParsed_thenThrowException() {
    assertInvalid(
        """
        coalesce-field: { table: Collections, field: id, derive-from: [acronym], strict: maybe }
        """,
        "maybe");
  }
}
