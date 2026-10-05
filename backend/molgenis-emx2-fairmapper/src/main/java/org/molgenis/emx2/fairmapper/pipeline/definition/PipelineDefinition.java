package org.molgenis.emx2.fairmapper.pipeline.definition;

import java.util.List;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;

/**
 * The contents of a pipeline file as plain data. Nothing has been fetched or created yet; the
 * components are built from it with a {@link PipelineContext}.
 *
 * @param preProcessors pre-processors in the order they are listed
 * @param postProcessors post-processors in the order they are listed
 */
public record PipelineDefinition(
    List<PreProcessorSpec> preProcessors, List<PostProcessorSpec> postProcessors) {

  public List<RdfPreProcessor> createPreProcessors(PipelineContext context) {
    return preProcessors.stream().map(spec -> spec.create(context)).toList();
  }

  public List<PostProcessor> createPostProcessors(PipelineContext context) {
    return postProcessors.stream().map(spec -> spec.create(context)).toList();
  }
}
