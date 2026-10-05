package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;

import org.molgenis.emx2.MolgenisException;

/** Builds valid pipeline files around the part a test is about. */
final class TestPipelineFiles {

  static final String SETTINGS =
      """
      schema: catalogue
      tables: [Collections]
      emx2:
        endpoint: http://localhost:8080
        token: token
      """;

  static final String EXTRACT = "- extract: { url: https://fdp.example.org }\n";
  static final String TRANSFORM = "- transform\n";

  private TestPipelineFiles() {}

  /** A pipeline file with the required settings and stages, plus {@code extraSteps}. */
  static String withSteps(String extraSteps) {
    return steps(EXTRACT + TRANSFORM + extraSteps);
  }

  /** A pipeline file with the required settings, a transform stage and {@code extractStep}. */
  static String withExtract(String extractStep) {
    return steps(extractStep.strip() + "\n" + TRANSFORM);
  }

  /** A pipeline file with the required settings and exactly the given {@code steps}. */
  static String steps(String steps) {
    return SETTINGS + "steps:\n" + steps.indent(2);
  }

  static void assertInvalid(String yaml, String expectedMessagePart) {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> PipelineFile.parse(yaml));
    assertTrue(
        exception.getMessage().contains(expectedMessagePart),
        () -> "Expected '" + expectedMessagePart + "' in: " + exception.getMessage());
  }
}
