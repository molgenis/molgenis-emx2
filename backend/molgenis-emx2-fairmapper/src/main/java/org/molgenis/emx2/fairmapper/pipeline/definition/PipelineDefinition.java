package org.molgenis.emx2.fairmapper.pipeline.definition;

import java.util.List;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;

/**
 * The contents of a pipeline file as plain data. Nothing has been fetched or created yet; the
 * components are built from it with a {@link PipelineContext}.
 *
 * @param schema name of the target schema, used both for the Transform and the Upload stage
 * @param tables names of the tables to harvest
 * @param output directory to dump the result of each stage to, or {@code null} for no dumps
 * @param emx2 the EMX2 instance to read schema metadata from and upload to
 * @param extract options of the Extract stage
 * @param preProcessors pre-processors in the order they are listed
 * @param postProcessors post-processors in the order they are listed
 * @param upload whether the Upload stage is part of the harvest
 */
public record PipelineDefinition(
    String schema,
    List<String> tables,
    String output,
    Emx2Settings emx2,
    ExtractSpec extract,
    List<PreProcessorSpec> preProcessors,
    List<PostProcessorSpec> postProcessors,
    boolean upload) {

  public List<RdfPreProcessor> createPreProcessors(PipelineContext context) {
    return preProcessors.stream().map(spec -> spec.create(context)).toList();
  }

  public List<PostProcessor> createPostProcessors(PipelineContext context) {
    return postProcessors.stream().map(spec -> spec.create(context)).toList();
  }
}
