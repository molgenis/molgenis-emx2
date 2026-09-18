package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.client.GraphqlClient;
import org.molgenis.emx2.fairmapper.client.GraphqlSchemaMetadataProvider;
import org.molgenis.emx2.fairmapper.extractors.CrawlSteps;
import org.molgenis.emx2.fairmapper.extractors.CrawlingRdfExtractor;
import org.molgenis.emx2.fairmapper.load.RemoteDataLoader;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipeline;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.DCATPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.StageCsvwPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

@CommandLine.Command(
    name = "harvest",
    description = "Harvest a given endpoint",
    mixinStandardHelpOptions = true)
public class Harvest implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(Harvest.class);
  public static final UUID HARVEST_ID = UUID.randomUUID();

  @CommandLine.Option(
      names = {"-r", "--rdf"},
      required = true,
      description = "FDP endpoint to harvest")
  private String rdf;

  @CommandLine.Option(
      names = {"-s", "--schema"},
      required = true,
      description = "Name of Molgenis schema that contains the desired tables")
  private String schemaName;

  @CommandLine.Option(
      names = {"-t", "--tables"},
      required = true,
      description = "Comma-separated list of table names to harvest")
  private String tablesArg;

  @CommandLine.Option(
      names = {"-o", "--output"},
      description = "Write intermediate post processing results to files")
  private String outputPath;

  @CommandLine.Option(
      names = {"-l", "--load"},
      description = "Write intermediate post processing results to files")
  private boolean enableLoading;

  @CommandLine.Option(
      names = {"--endpoint"},
      required = true,
      description = "Base URL of the remote emx2 instance")
  private String endpoint;

  @CommandLine.Option(
      names = {"--token"},
      required = true,
      description = "Authentication token for the remote emx2 instance")
  private String token;

  @Override
  public void run() {
    logger.info("Starting harvest with ID: {}", HARVEST_ID);

    SchemaMetadataProvider schemaMetadataProvider = getSchemaMetadataProvider();
    SchemaMetadata schema =
        Optional.ofNullable(schemaMetadataProvider.getSchemaMetadata(schemaName))
            .orElseThrow(() -> new MolgenisException("Schema not found: " + schemaName));

    HarvestingPipelineConfig.Builder builder =
        new HarvestingPipelineConfig.Builder(
                URI.create(rdf),
                schemaName,
                schemaMetadataProvider,
                new CrawlingRdfExtractor().withCrawlSteps(CrawlSteps.FDP.steps()),
                new SparqlSelectRdfTransformer(new TableQueryGenerator()))
            .setTables(this.tablesArg.split(","))
            .withPostProcessors(new DCATPostProcessor(new GraphqlClient(endpoint, token), schema))
            .withPreProcessors(
                new TemporalRdfPreProcessor(),
                new TypicalAgeRdfPreProcessor(),
                new StageCsvwPreProcessor());

    if (outputPath != null) {
      builder.withDumpEnabled(outputPath);
    }

    if (enableLoading) {
      builder.withDataLoader(new RemoteDataLoader(endpoint, token, schemaName));
    }

    runPipeline(builder);
  }

  SchemaMetadataProvider getSchemaMetadataProvider() {
    return new GraphqlSchemaMetadataProvider(new GraphqlClient(endpoint, token));
  }

  public void runPipeline(HarvestingPipelineConfig.Builder builder) {
    new HarvestingPipeline(builder.build()).execute();
  }
}
