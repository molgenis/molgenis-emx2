package org.molgenis.emx2.fairmapper.cli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.fairmapper.extractors.FdpRdfExtractor;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.DCATPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.schemas.SchemaFetcher;
import org.molgenis.emx2.fairmapper.tasks.RemoteDataLoader;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import picocli.CommandLine;

class HarvestRemoteTest {

  private static final String RDF_ENDPOINT = "https://example.org/fdp";
  private static final String REMOTE_ENDPOINT = "https://example.org/emx2";
  private static final String TOKEN = "some-token";
  private static final String SCHEMA_NAME = "RemoteSchema";

  @Test
  void shouldPassRdfSchemaAndTablesIntoConfig() {
    HarvestingPipelineConfig config = runAndCaptureConfig("TableA,TableB");

    assertEquals(URI.create(RDF_ENDPOINT), config.rdf());
    assertEquals(SCHEMA_NAME, config.schemaName());
    assertEquals(List.of("TableA", "TableB"), config.tables());
  }

  @Test
  void shouldConfigureFdpExtractorAndSparqlTransformer() {
    HarvestingPipelineConfig config = runAndCaptureConfig("TableA");

    assertInstanceOf(FdpRdfExtractor.class, config.extractor());
    assertInstanceOf(SparqlSelectRdfTransformer.class, config.transformer());
  }

  @Test
  void shouldConfigureDcatPreAndPostProcessors() {
    HarvestingPipelineConfig config = runAndCaptureConfig("TableA");

    assertEquals(1, config.postProcessors().size());
    assertInstanceOf(DCATPostProcessor.class, config.postProcessors().getFirst());

    assertEquals(2, config.preProcessors().size());
    assertInstanceOf(TemporalRdfPreProcessor.class, config.preProcessors().get(0));
    assertInstanceOf(TypicalAgeRdfPreProcessor.class, config.preProcessors().get(1));
  }

  @Test
  void shouldEnableDumpingWithGivenOutputPathWhenOutputOptionProvided() {
    HarvestingPipelineConfig config = runAndCaptureConfig("TableA", "-o", "/tmp/harvest-output");

    assertTrue(config.dumpEnabled());
    assertEquals("/tmp/harvest-output", config.outputPath());
  }

  @Test
  void shouldNotEnableDumpingWhenOutputOptionOmitted() {
    HarvestingPipelineConfig config = runAndCaptureConfig("TableA");

    assertFalse(config.dumpEnabled());
    assertNull(config.outputPath());
  }

  @Test
  void shouldNotEnableDataLoadingWhenLoadOptionOmitted() {
    HarvestingPipelineConfig config = runAndCaptureConfig("TableA");

    assertFalse(config.loadEnabled());
  }

  @Test
  void shouldEnableDataLoadingWhenLoadOptionProvided() {
    HarvestingPipelineConfig config = runAndCaptureConfig("TableA", "-l");

    assertTrue(config.loadEnabled());
    assertInstanceOf(RemoteDataLoader.class, config.loader());
  }

  private HarvestingPipelineConfig runAndCaptureConfig(String tables, String... extraArgs) {
    HarvestRemote harvest = spy(new HarvestRemote());
    doNothing().when(harvest).runPipeline(any());

    SchemaFetcher schemaFetcher = mock(SchemaFetcher.class);
    when(schemaFetcher.fetch(SCHEMA_NAME)).thenReturn(Optional.of(new SchemaMetadata(SCHEMA_NAME)));
    doReturn(schemaFetcher).when(harvest).schemaFetcher();

    String[] args =
        Stream.concat(
                Stream.of(
                    "-r", RDF_ENDPOINT,
                    "-s", SCHEMA_NAME,
                    "-t", tables,
                    "--endpoint", REMOTE_ENDPOINT,
                    "--token", TOKEN),
                Arrays.stream(extraArgs))
            .toArray(String[]::new);
    new CommandLine(harvest).execute(args);

    ArgumentCaptor<HarvestingPipelineConfig.Builder> captor =
        ArgumentCaptor.forClass(HarvestingPipelineConfig.Builder.class);
    verify(harvest).runPipeline(captor.capture());
    return captor.getValue().build();
  }
}
