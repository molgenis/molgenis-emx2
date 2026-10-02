package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.ontologies.ResolveOntologyPostProcessor;

/**
 * Pipeline file entry for {@link ResolveOntologyPostProcessor}. Takes no arguments; the target
 * schema and EMX2 client come from the {@link PipelineContext}.
 *
 * <pre>
 * - resolve-ontologies
 * </pre>
 */
@JsonTypeName("resolve-ontologies")
public record ResolveOntologiesSpec() implements PostProcessorSpec {

  @Override
  public PostProcessor create(PipelineContext context) {
    return new ResolveOntologyPostProcessor(context.schema(), context.client());
  }
}
