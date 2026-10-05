package org.molgenis.emx2.fairmapper.pipeline.definition;

import java.util.List;
import org.molgenis.emx2.SchemaMetadataProvider;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.fairmapper.upload.RemoteDataUploader;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;

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

  /**
   * Creates the configuration of the harvest this definition describes. Fetches the metadata of the
   * target schema, which the Transform stage and several post-processors need.
   *
   * @param client client for the EMX2 instance in {@link #emx2()}
   * @param schemaMetadataProvider provides the metadata of the target schema
   */
  public HarvestingPipelineConfig.Builder createConfig(
      GraphqlClient client, SchemaMetadataProvider schemaMetadataProvider) {
    PipelineContext context =
        new PipelineContext(client, schemaMetadataProvider.getSchemaMetadata(schema));

    HarvestingPipelineConfig.Builder builder =
        new HarvestingPipelineConfig.Builder(
                extract.url(),
                schema,
                schemaMetadataProvider,
                extract.create(),
                new SparqlSelectRdfTransformer(new TableQueryGenerator()))
            .setTables(tables.toArray(String[]::new))
            .withPreProcessors(createPreProcessors(context).toArray(RdfPreProcessor[]::new))
            .withPostProcessors(createPostProcessors(context).toArray(PostProcessor[]::new));

    if (output != null) {
      builder.withDumpEnabled(output);
    }
    if (upload) {
      builder.withDataUploader(new RemoteDataUploader(emx2.endpoint(), emx2.token(), schema));
    }
    return builder;
  }

  public List<RdfPreProcessor> createPreProcessors(PipelineContext context) {
    return preProcessors.stream().map(spec -> spec.create(context)).toList();
  }

  public List<PostProcessor> createPostProcessors(PipelineContext context) {
    return postProcessors.stream().map(spec -> spec.create(context)).toList();
  }
}
