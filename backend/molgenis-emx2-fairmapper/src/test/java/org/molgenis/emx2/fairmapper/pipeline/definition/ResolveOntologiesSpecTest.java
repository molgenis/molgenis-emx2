package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.assertInvalid;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.parse;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;
import org.molgenis.emx2.fairmapper.postprocessing.ontologies.ResolveOntologyPostProcessor;

class ResolveOntologiesSpecTest {

  @Test
  void givenBareName_whenParsed_thenCreateSpec() {
    assertEquals(new ResolveOntologiesSpec(), parse("resolve-ontologies"));
  }

  @Test
  void givenEmptyArguments_whenParsed_thenSameAsBareName() {
    assertEquals(new ResolveOntologiesSpec(), parse("resolve-ontologies: {}"));
  }

  @Test
  void givenArgument_whenParsed_thenThrowException() {
    assertInvalid("resolve-ontologies: { table: Collections }", "table");
  }

  @Test
  void whenCreated_thenUseContext() {
    PipelineContext context =
        new PipelineContext(
            new GraphqlClient("http://localhost:8080", "token"), new SchemaMetadata("test"));

    assertInstanceOf(
        ResolveOntologyPostProcessor.class, new ResolveOntologiesSpec().create(context));
  }
}
