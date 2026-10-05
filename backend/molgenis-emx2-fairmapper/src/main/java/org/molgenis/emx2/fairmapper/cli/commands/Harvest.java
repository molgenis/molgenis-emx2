package org.molgenis.emx2.fairmapper.cli.commands;

import java.nio.file.Path;
import java.util.UUID;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.client.CachingSchemaMetadataProvider;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;
import org.molgenis.emx2.fairmapper.client.GraphqlSchemaMetadataProvider;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipeline;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.pipeline.definition.PipelineDefinition;
import org.molgenis.emx2.fairmapper.pipeline.definition.PipelineFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
    name = "harvest",
    description = "Run the harvest described by a pipeline file",
    mixinStandardHelpOptions = true)
public class Harvest implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Harvest.class);
  public static final UUID HARVEST_ID = UUID.randomUUID();

  @CommandLine.Option(
      names = {"-c", "--config"},
      required = true,
      description = "Pipeline file (YAML) that describes the harvest")
  private Path config;

  @Override
  public void run() {
    logger.info("Starting harvest with ID: {}", HARVEST_ID);

    PipelineDefinition definition = PipelineFile.read(config);
    GraphqlClient client =
        new GraphqlClient(definition.emx2().endpoint(), definition.emx2().token());

    runPipeline(definition.createConfig(client, getSchemaMetadataProvider(client)));
  }

  SchemaMetadataProvider getSchemaMetadataProvider(GraphqlClient client) {
    return new CachingSchemaMetadataProvider(new GraphqlSchemaMetadataProvider(client));
  }

  public void runPipeline(HarvestingPipelineConfig.Builder builder) {
    new HarvestingPipeline(builder.build()).execute();
  }
}
