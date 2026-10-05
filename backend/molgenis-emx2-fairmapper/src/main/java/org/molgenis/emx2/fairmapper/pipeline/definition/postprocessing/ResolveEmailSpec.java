package org.molgenis.emx2.fairmapper.pipeline.definition.postprocessing;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.molgenis.emx2.fairmapper.pipeline.definition.PipelineContext;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.ResolveEmailPostProcessor;

/**
 * Pipeline file entry for {@link ResolveEmailPostProcessor}. Takes no arguments; the target schema
 * and EMX2 client come from the {@link PipelineContext}.
 *
 * <pre>
 * - email
 * </pre>
 */
@JsonTypeName("resolve-email")
public record ResolveEmailSpec() implements PostProcessorSpec {

  @Override
  public PostProcessor create(PipelineContext context) {
    return new ResolveEmailPostProcessor(context.schema());
  }
}
