package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.ResolveMissingPkPostProcessor;

/**
 * Pipeline file entry for {@link ResolveMissingPkPostProcessor}. Takes no arguments; the target
 * schema comes from the {@link PipelineContext}.
 *
 * <pre>
 * - resolve-missing-pk
 * </pre>
 */
@JsonTypeName("resolve-missing-pk")
public record ResolveMissingPkSpec() implements PostProcessorSpec {

  @Override
  public PostProcessor create(PipelineContext context) {
    return new ResolveMissingPkPostProcessor(context.schema());
  }
}
