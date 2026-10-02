package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.molgenis.emx2.MolgenisException;

final class PostProcessorSpecParsing {

  private PostProcessorSpecParsing() {}

  static PostProcessorSpec parse(String entry) {
    List<PostProcessorSpec> specs = PipelineFile.parse(pipelineFile(entry)).postProcessors();
    assertEquals(1, specs.size());
    return specs.getFirst();
  }

  static void assertInvalid(String entry, String expectedMessagePart) {
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
