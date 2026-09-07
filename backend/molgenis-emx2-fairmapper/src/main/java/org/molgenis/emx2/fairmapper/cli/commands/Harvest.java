package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import java.util.UUID;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;
import org.molgenis.emx2.fairmapper.client.GraphqlDatabase;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipeline;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.DCATPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
    name = "harvest",
    description = "Harvest a given endpoint into a remote emx2 instance",
    mixinStandardHelpOptions = true)
public class Harvest implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Harvest.class);
  public static final UUID HARVEST_ID = UUID.randomUUID();

  @CommandLine.Option(
      names = {"-r", "--rdf"},
      required = true,
      description = "FDP endpoint to harvest")
  protected String rdf;

  @CommandLine.Option(
      names = {"-s", "--schema"},
      required = true,
      description = "Name of Molgenis schema that contains the desired tables")
  protected String schemaName;

  @CommandLine.Option(
      names = {"-t", "--tables"},
      required = true,
      description = "Comma-separated list of table names to harvest")
  protected String tablesArg;

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "Write intermediate post processing results to files")
  protected String outputPath;

  @CommandLine.Option(
      names = {"-l", "--load"},
      description = "Load harvested data into the target schema")
  protected boolean enableLoading;

  @CommandLine.Option(
      names = {"--endpoint"},
      required = true,
      description = "Base URL of the remote emx2 instance")
  protected String endpoint;

  @CommandLine.Option(
      names = {"--token"},
      required = true,
      description = "Authentication token for the remote emx2 instance")
  protected String token;

  @Override
  public void run() {
    logger.info("Starting harvest with ID: {}", HARVEST_ID);

    URI rdfUri = URI.create(rdf);
    String[] tables = tablesArg.split(",");

    HarvestingPipelineConfig.Builder builder =
        HarvestingPipelineConfig.Builder.remoteConfig(endpoint, token, rdfUri, schemaName, tables)
            .withPostProcessors(
                new DCATPostProcessor(fetchSchemaMetadata(), new GraphqlClient(endpoint, token)))
            .withPreProcessors(new TemporalRdfPreProcessor(), new TypicalAgeRdfPreProcessor());

    if (outputPath != null) {
      builder.withDumpEnabled(outputPath);
    }

    if (!enableLoading) {
      builder.withLoader(null);
    }

    runPipeline(builder);
  }

  public void runPipeline(HarvestingPipelineConfig.Builder builder) {
    new HarvestingPipeline(builder.build()).execute();
  }

  private SchemaMetadata fetchSchemaMetadata() {
    return database().getSchemaMetadata(schemaName);
  }

  protected Database database() {
    return new GraphqlDatabase(new GraphqlClient(endpoint, token));
  }
}
