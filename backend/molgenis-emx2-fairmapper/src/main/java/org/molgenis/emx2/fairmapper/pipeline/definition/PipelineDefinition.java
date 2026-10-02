package org.molgenis.emx2.fairmapper.pipeline.definition;

import java.util.List;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;

/**
 * The contents of a pipeline file as plain data. Nothing has been fetched or created yet; the
 * components are built from it with a {@link PipelineContext}.
 *
 * @param postProcessors post-processors in the order they are listed
 */
public record PipelineDefinition(List<PostProcessorSpec> postProcessors) {

  public List<PostProcessor> createPostProcessors(PipelineContext context) {
    return postProcessors.stream().map(spec -> spec.create(context)).toList();
  }
}
