package org.molgenis.emx2.fairmapper.pipeline.definition.postprocessing;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.postprocessing.PostProcessorSpecParsing.assertInvalid;
import static org.molgenis.emx2.fairmapper.pipeline.definition.postprocessing.PostProcessorSpecParsing.parse;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;
import org.molgenis.emx2.fairmapper.pipeline.definition.PipelineContext;
import org.molgenis.emx2.fairmapper.postprocessing.ResolveEmailPostProcessor;

class ResolveEmailSpecTest {

  @Test
  void givenBareName_whenParsed_thenCreateSpec() {
    assertEquals(new ResolveEmailSpec(), parse("resolve-email"));
  }

  @Test
  void givenEmptyArguments_whenParsed_thenSameAsBareName() {
    assertEquals(new ResolveEmailSpec(), parse("resolve-email: {}"));
  }

  @Test
  void givenArgument_whenParsed_thenThrowException() {
    assertInvalid("resolve-email: { table: Collections }", "table");
  }

  @Test
  void whenCreated_thenUseContext() {
    PipelineContext context =
        new PipelineContext(
            new GraphqlClient("http://localhost:8080", "token"), new SchemaMetadata("test"));

    assertInstanceOf(ResolveEmailPostProcessor.class, new ResolveEmailSpec().create(context));
  }
}
