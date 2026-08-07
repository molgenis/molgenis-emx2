package org.molgenis.emx2.fairmapper.cli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.fairmapper.extractors.FdpRdfExtractor;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.DCATPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import picocli.CommandLine;

class HarvestTest {

  private static final String RDF_ENDPOINT = "https://example.org/fdp";

  private Schema schema;

  @BeforeEach
  void setUp() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(getClass().getSimpleName());
    schema.create(
        TableMetadata.table("TableA", Column.column("id").setType(ColumnType.STRING).setPkey()),
        TableMetadata.table("TableB", Column.column("id").setType(ColumnType.STRING).setPkey()));
  }

  @Test
  void shouldPassRdfSchemaAndTablesIntoConfig() {
    HarvestingPipelineConfig config = runAndCaptureConfig(RDF_ENDPOINT, "TableA,TableB");

    assertEquals(URI.create(RDF_ENDPOINT), config.rdf());
    assertEquals(schema.getName(), config.schema().getName());
    assertArrayEquals(new String[] {"TableA", "TableB"}, config.tables());
  }

  @Test
  void shouldConfigureFdpExtractorAndSparqlTransformer() {
    HarvestingPipelineConfig config = runAndCaptureConfig(RDF_ENDPOINT, "TableA");

    assertInstanceOf(FdpRdfExtractor.class, config.extractor());
    assertInstanceOf(SparqlSelectRdfTransformer.class, config.transformer());
  }

  @Test
  void shouldConfigureDcatPreAndPostProcessors() {
    HarvestingPipelineConfig config = runAndCaptureConfig(RDF_ENDPOINT, "TableA");

    assertEquals(1, config.postProcessors().size());
    assertInstanceOf(DCATPostProcessor.class, config.postProcessors().get(0));

    assertEquals(2, config.preProcessors().size());
    assertInstanceOf(TemporalRdfPreProcessor.class, config.preProcessors().get(0));
    assertInstanceOf(TypicalAgeRdfPreProcessor.class, config.preProcessors().get(1));
  }

  @Test
  void shouldEnableDumpingWithGivenOutputPathWhenOutputOptionProvided() {
    HarvestingPipelineConfig config =
        runAndCaptureConfig(RDF_ENDPOINT, "TableA", "-o", "/tmp/harvest-output");

    assertTrue(config.dumpEnabled());
    assertEquals("/tmp/harvest-output", config.outputPath());
  }

  @Test
  void shouldNotEnableDumpingWhenOutputOptionOmitted() {
    HarvestingPipelineConfig config = runAndCaptureConfig(RDF_ENDPOINT, "TableA");

    assertFalse(config.dumpEnabled());
    assertNull(config.outputPath());
  }

  @Test
  void shouldNeverEnableDataLoading() {
    HarvestingPipelineConfig config = runAndCaptureConfig(RDF_ENDPOINT, "TableA");

    assertFalse(config.loadDataEnabled());
  }

  private HarvestingPipelineConfig runAndCaptureConfig(
      String rdf, String tables, String... extraArgs) {
    Harvest harvest = spy(new Harvest());
    doNothing().when(harvest).runPipeline(any());

    String[] args =
        Stream.concat(Stream.of(rdf, schema.getName(), tables), Arrays.stream(extraArgs))
            .toArray(String[]::new);
    new CommandLine(harvest).execute(args);

    ArgumentCaptor<HarvestingPipelineConfig.Builder> captor =
        ArgumentCaptor.forClass(HarvestingPipelineConfig.Builder.class);
    verify(harvest).runPipeline(captor.capture());
    return captor.getValue().build();
  }
}
