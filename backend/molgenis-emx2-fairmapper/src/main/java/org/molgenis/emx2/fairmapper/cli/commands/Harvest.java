package org.molgenis.emx2.fairmapper.cli.commands;

import static org.molgenis.emx2.fairmapper.extractors.FdpCrawlSteps.CATALOGS;
import static org.molgenis.emx2.fairmapper.extractors.FdpCrawlSteps.CSVW;
import static org.molgenis.emx2.fairmapper.extractors.FdpCrawlSteps.DATASETS;
import static org.molgenis.emx2.fairmapper.extractors.FdpCrawlSteps.DISTRIBUTIONS;

import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.extractors.FdpRdfExtractor;
import org.molgenis.emx2.fairmapper.extractors.RdfExtractor;
import org.molgenis.emx2.fairmapper.extractors.RemoteRdfExtractor;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipeline;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.DCATPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.SqlDatabase;
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

  @Override
  public void run() {
    logger.info("Starting harvest with ID: {}", HARVEST_ID);

    Database database = setupDatabase();
    Schema schema = validateSchema(database);
    String[] tables = validateTables(schema);

    URI rdfURI = getRdf();

    RdfExtractor extractor =
        new FdpRdfExtractor(new RemoteRdfExtractor())
            .withCrawlSteps(CATALOGS, DATASETS, DISTRIBUTIONS, CSVW);
    SparqlSelectRdfTransformer transformer =
        new SparqlSelectRdfTransformer(
            new TableQueryGenerator(), schema.getMetadata(), List.of(tables));

    HarvestingPipelineConfig.Builder builder =
        new HarvestingPipelineConfig.Builder(rdfURI, schema, extractor, transformer)
            .setTables(tables)
            .withPostProcessors(new DCATPostProcessor(schema.getMetadata()))
            .withPreProcessors(new TemporalRdfPreProcessor(), new TypicalAgeRdfPreProcessor());

    if (outputPath != null) {
      builder.withDumpEnabled(outputPath);
    }

    if (enableLoading) {
      builder.enableDataLoading();
    }

    runPipeline(builder);
  }

  public void runPipeline(HarvestingPipelineConfig.Builder builder) {
    new HarvestingPipeline(builder.build()).execute();
  }

  @NotNull
  private URI getRdf() {
    return URI.create(rdf);
  }

  private static Database setupDatabase() {
    logger.info("Accessing database");
    SqlDatabase database = new SqlDatabase(false);
    database.becomeAdmin();
    return database;
  }

  private Schema validateSchema(Database database) {
    logger.info("Retrieving schema information: {}", schemaName);
    Schema schema = database.getSchema(schemaName);
    if (schema == null) {
      throw new MolgenisException("Schema not found: " + schemaName);
    }
    return schema;
  }

  private String[] validateTables(Schema schema) {
    logger.info("Validating table names: {}", tablesArg);
    String[] tables = this.tablesArg.split(",");
    for (String table : tables) {
      if (schema.getTable(table) == null) {
        throw new MolgenisException("Table not found: " + table);
      }
    }
    return tables;
  }
}
