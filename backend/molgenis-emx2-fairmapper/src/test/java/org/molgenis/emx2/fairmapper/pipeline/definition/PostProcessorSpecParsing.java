package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

final class PostProcessorSpecParsing {

  private PostProcessorSpecParsing() {}

  static PostProcessorSpec parse(String entry) {
    List<PostProcessorSpec> specs = PipelineFile.parse(pipelineFile(entry)).postProcessors();
    assertEquals(1, specs.size());
    return specs.getFirst();
  }

  static void assertInvalid(String entry, String expectedMessagePart) {
    TestPipelineFiles.assertInvalid(pipelineFile(entry), expectedMessagePart);
  }

  private static String pipelineFile(String entry) {
    return TestPipelineFiles.withSteps(
        """
        - postprocessing:
            - %s
        """
            .formatted(entry.strip().replace("\n", "\n      ")));
  }
}
