package org.molgenis.emx2.fairmapper.cli.commands;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.extractors.CrawlingRdfExtractor;
import org.molgenis.emx2.fairmapper.pipeline.HarvestingPipelineConfig;
import org.molgenis.emx2.fairmapper.postprocessing.CoalesceFieldPostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.DropMissingPkRowPostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.ResolveMissingPkPostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.ResolveStaticFieldPostProcessor;
import org.molgenis.emx2.fairmapper.postprocessing.ontologies.ResolveOntologyPostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.StageCsvwPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TemporalRdfPreProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.TypicalAgeRdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import picocli.CommandLine;

class HarvestTest {

  private static final Path EXAMPLE_PIPELINE_FILE = Path.of("examples/stage-fdp.yml");

  private static final String SETTINGS =
      """
      schema: catalogue
      tables: [TableA, TableB]
      emx2:
        endpoint: http://localhost:8080
        token: token123
      """;

  @TempDir private Path dir;

  @Test
  void shouldPassSourceSchemaAndTablesIntoConfig() throws IOException {
    HarvestingPipelineConfig config = runAndCaptureConfig(withSteps(""));

    assertEquals(URI.create("https://example.org/fdp"), config.rdf());
    assertEquals("catalogue", config.schemaName());
    assertEquals(List.of("TableA", "TableB"), config.tables());
  }

  @Test
  void shouldConfigureCrawlingExtractorAndSparqlTransformer() throws IOException {
    HarvestingPipelineConfig config = runAndCaptureConfig(withSteps(""));

    assertInstanceOf(CrawlingRdfExtractor.class, config.extractor());
    assertInstanceOf(SparqlSelectRdfTransformer.class, config.transformer());
  }

  @Test
  void shouldCreatePreAndPostProcessorsListedInPipelineFile() throws IOException {
    HarvestingPipelineConfig config =
        runAndCaptureConfig(
            withSteps(
                """
                - preprocessing: [stage-csvw, temporal]
                - postprocessing:
                    - resolve-missing-pk
                    - resolve-static: { table: TableA, field: type, value: x }
                """));

    assertTypes(
        List.of(StageCsvwPreProcessor.class, TemporalRdfPreProcessor.class),
        config.preProcessors());
    assertTypes(
        List.of(ResolveMissingPkPostProcessor.class, ResolveStaticFieldPostProcessor.class),
        config.postProcessors());
  }

  @Test
  void shouldNotConfigurePreOrPostProcessorsWhenStagesAreLeftOut() throws IOException {
    HarvestingPipelineConfig config = runAndCaptureConfig(withSteps(""));

    assertEquals(List.of(), config.preProcessors());
    assertEquals(List.of(), config.postProcessors());
  }

  @Test
  void shouldEnableDumpingWhenOutputIsSet() throws IOException {
    HarvestingPipelineConfig config =
        runAndCaptureConfig("output: /tmp/harvest-output\n" + withSteps(""));

    assertTrue(config.dumpEnabled());
    assertEquals("/tmp/harvest-output", config.outputPath());
  }

  @Test
  void shouldNotEnableDumpingWhenOutputIsLeftOut() throws IOException {
    HarvestingPipelineConfig config = runAndCaptureConfig(withSteps(""));

    assertFalse(config.dumpEnabled());
    assertNull(config.outputPath());
  }

  @Test
  void shouldEnableUploadWhenUploadStageIsListed() throws IOException {
    HarvestingPipelineConfig config = runAndCaptureConfig(withSteps("- upload\n"));

    assertTrue(config.uploadEnabled());
  }

  @Test
  void shouldNotEnableUploadWhenUploadStageIsLeftOut() throws IOException {
    HarvestingPipelineConfig config = runAndCaptureConfig(withSteps(""));

    assertFalse(config.uploadEnabled());
  }

  @Test
  void shouldNotRunPipelineForInvalidPipelineFile() throws IOException {
    Harvest harvest = stubbedHarvest();

    int exitCode =
        new CommandLine(harvest).execute("--config", write(SETTINGS + "steps: []").toString());

    assertNotEquals(0, exitCode);
    verify(harvest, never()).runPipeline(any());
  }

  @Test
  void shouldRequireConfigOption() {
    Harvest harvest = stubbedHarvest();

    int exitCode = new CommandLine(harvest).execute();

    assertNotEquals(0, exitCode);
    verify(harvest, never()).runPipeline(any());
  }

  @Test
  void shouldConfigureDcatHarvestFromExamplePipelineFile() {
    HarvestingPipelineConfig config = runAndCaptureConfig(EXAMPLE_PIPELINE_FILE);

    assertEquals(List.of("Catalogues", "Collections", "Organisations"), config.tables());
    assertTypes(
        List.of(
            TemporalRdfPreProcessor.class,
            TypicalAgeRdfPreProcessor.class,
            StageCsvwPreProcessor.class),
        config.preProcessors());
    assertTypes(
        List.of(
            CoalesceFieldPostProcessor.class,
            CoalesceFieldPostProcessor.class,
            CoalesceFieldPostProcessor.class,
            ResolveStaticFieldPostProcessor.class,
            ResolveStaticFieldPostProcessor.class,
            ResolveOntologyPostProcessor.class,
            ResolveMissingPkPostProcessor.class,
            DropMissingPkRowPostProcessor.class),
        config.postProcessors());
    assertFalse(config.uploadEnabled());
    assertFalse(config.dumpEnabled());
  }

  private static String withSteps(String extraSteps) {
    return SETTINGS
        + "steps:\n"
        + ("- extract: { url: https://example.org/fdp, crawl: fdp }\n- transform\n" + extraSteps)
            .indent(2);
  }

  private static void assertTypes(List<Class<?>> expected, List<?> actual) {
    assertEquals(expected, actual.stream().map(Object::getClass).toList());
  }

  private HarvestingPipelineConfig runAndCaptureConfig(String pipelineFile) throws IOException {
    return runAndCaptureConfig(write(pipelineFile));
  }

  private static HarvestingPipelineConfig runAndCaptureConfig(Path pipelineFile) {
    Harvest harvest = stubbedHarvest();
    new CommandLine(harvest).execute("--config", pipelineFile.toString());

    ArgumentCaptor<HarvestingPipelineConfig.Builder> captor =
        ArgumentCaptor.forClass(HarvestingPipelineConfig.Builder.class);
    verify(harvest).runPipeline(captor.capture());
    return captor.getValue().build();
  }

  private static Harvest stubbedHarvest() {
    SchemaMetadata schema = new SchemaMetadata("catalogue");
    Harvest harvest = spy(new Harvest());
    doReturn((SchemaMetadataProvider) schemaName -> schema)
        .when(harvest)
        .getSchemaMetadataProvider(any());
    doNothing().when(harvest).runPipeline(any());
    return harvest;
  }

  private Path write(String pipelineFile) throws IOException {
    Path file = dir.resolve("pipeline.yaml");
    Files.writeString(file, pipelineFile);
    return file;
  }
}
