package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.preprocessing.StageCsvwPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;

class PreProcessorSpecTest {

  private static final PipelineContext CONTEXT = new PipelineContext(null, null);

  static Stream<Arguments> preProcessors() {
    return Stream.of(
        Arguments.of("temporal", new TemporalSpec(), TemporalRdfPreProcessor.class),
        Arguments.of("typical-age", new TypicalAgeSpec(), TypicalAgeRdfPreProcessor.class),
        Arguments.of("stage-csvw", new StageCsvwSpec(), StageCsvwPreProcessor.class));
  }

  @ParameterizedTest
  @MethodSource("preProcessors")
  void givenBareName_whenParsed_thenCreateSpec(String name, PreProcessorSpec expected) {
    assertEquals(List.of(expected), parse("[" + name + "]"));
  }

  @ParameterizedTest
  @MethodSource("preProcessors")
  void givenEmptyArguments_whenParsed_thenSameAsBareName(String name, PreProcessorSpec expected) {
    assertEquals(List.of(expected), parse("[" + name + ": {}]"));
  }

  @ParameterizedTest
  @MethodSource("preProcessors")
  void givenArgument_whenParsed_thenThrowException(String name) {
    assertInvalid("[" + name + ": { table: Collections }]", "table");
  }

  @ParameterizedTest
  @MethodSource("preProcessors")
  void whenCreated_thenCreateMatchingPreProcessor(
      String name, PreProcessorSpec spec, Class<?> expectedType) {
    assertInstanceOf(expectedType, spec.create(CONTEXT));
  }

  @Test
  void givenSeveralPreProcessors_whenParsed_thenKeepListedOrder() {
    assertEquals(
        List.of(new StageCsvwSpec(), new TemporalSpec(), new TypicalAgeSpec()),
        parse("[stage-csvw, temporal, typical-age]"));
  }

  @Test
  void givenUnknownName_whenParsed_thenThrowException() {
    assertInvalid("[temporl]", "temporl");
  }

  private static List<PreProcessorSpec> parse(String list) {
    return PipelineFile.parse(pipelineFile(list)).preProcessors();
  }

  private static void assertInvalid(String list, String expectedMessagePart) {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> PipelineFile.parse(pipelineFile(list)));
    assertTrue(
        exception.getMessage().contains(expectedMessagePart),
        () -> "Expected '" + expectedMessagePart + "' in: " + exception.getMessage());
  }

  private static String pipelineFile(String list) {
    return """
        steps:
          - preprocessing: %s
        """
        .formatted(list);
  }
}
