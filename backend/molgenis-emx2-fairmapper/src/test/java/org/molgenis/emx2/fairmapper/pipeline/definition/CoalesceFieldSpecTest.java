package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.MolgenisException;

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

  /** Parses a single post-processing entry by wrapping it in a minimal pipeline file. */
  private static PostProcessorSpec parse(String entry) {
    List<PostProcessorSpec> specs = PipelineFile.parse(pipelineFile(entry)).postProcessors();
    assertEquals(1, specs.size());
    return specs.getFirst();
  }

  private static void assertInvalid(String entry, String expectedMessagePart) {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> PipelineFile.parse(pipelineFile(entry)));
    assertTrue(
        exception.getMessage().contains(expectedMessagePart),
        () -> "Expected '" + expectedMessagePart + "' in: " + exception.getMessage());
  }

  private static String pipelineFile(String entry) {
    return """
        steps:
          - postprocessing:
              - %s
        """
        .formatted(entry.strip().replace("\n", "\n        "));
  }
}
