package org.molgenis.emx2.fairmapper.cli.commands;

import java.net.URI;
import java.util.UUID;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipeline;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
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
    throw new UnsupportedOperationException("Not yet implemented");
  }

  public void runPipeline(HarvestingPipelineConfig.Builder builder) {
    new HarvestingPipeline(builder.build()).execute();
  }

  /**
   * Builds the mode-specific part of the config (schema access, extractor, transformer, loader).
   * Shared concerns (pre/post processors, dump/load toggles) are applied on top by {@link #run()}.
   */
  protected abstract HarvestingPipelineConfig.Builder buildConfigBuilder(
      URI rdfUri, String[] tables);
}
