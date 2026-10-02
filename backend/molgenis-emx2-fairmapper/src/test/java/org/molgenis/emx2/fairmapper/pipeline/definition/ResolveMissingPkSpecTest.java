package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.assertInvalid;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.parse;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.postprocessing.ResolveMissingPkPostProcessor;

class ResolveMissingPkSpecTest {

  @Test
  void givenBareName_whenParsed_thenCreateSpec() {
    assertEquals(new ResolveMissingPkSpec(), parse("resolve-missing-pk"));
  }

  @Test
  void givenEmptyArguments_whenParsed_thenSameAsBareName() {
    assertEquals(new ResolveMissingPkSpec(), parse("resolve-missing-pk: {}"));
  }

  @Test
  void givenArgument_whenParsed_thenThrowException() {
    assertInvalid("resolve-missing-pk: { tables: [Collections] }", "tables");
  }

  @Test
  void whenCreated_thenUseContext() {
    PipelineContext context = new PipelineContext(null, new SchemaMetadata("test"));

    assertInstanceOf(
        ResolveMissingPkPostProcessor.class, new ResolveMissingPkSpec().create(context));
  }
}
