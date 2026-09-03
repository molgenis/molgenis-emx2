package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import java.util.UUID;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipeline;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.DCATPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.schemas.SchemaFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Shared shape for the {@code harvest local} and {@code harvest remote} subcommands: the options
 * common to both, and the parts of the run that don't depend on where the schema comes from or
 * where harvested data ends up.
 */
public abstract class AbstractHarvestCommand implements Runnable {

  protected static final Logger logger = LoggerFactory.getLogger(AbstractHarvestCommand.class);
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

  @Override
  public void run() {
    logger.info("Starting harvest with ID: {}", HARVEST_ID);

    URI rdfUri = URI.create(rdf);
    String[] tables = tablesArg.split(",");

    HarvestingPipelineConfig.Builder builder =
        configBuilder(rdfUri, tables)
            .withPostProcessors(new DCATPostProcessor(fetchSchemaMetadata()))
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
    return schemaFetcher()
        .fetch(schemaName)
        .orElseThrow(() -> new MolgenisException("Schema not found: " + schemaName));
  }

  protected abstract SchemaFetcher schemaFetcher();

  protected abstract HarvestingPipelineConfig.Builder configBuilder(URI rdfUri, String[] tables);
}
