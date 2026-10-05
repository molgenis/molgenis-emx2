package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.TestPipelineFiles.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

class PipelineFileTest {

  private static final String TABLE_NAME = "Collections";
  private static final PipelineContext CONTEXT = new PipelineContext(null, null);

  @Test
  void shouldReadCompletePipelineFile() {
    PipelineDefinition definition =
        PipelineFile.parse(
            """
            schema: catalogue
            tables: [Catalogues, Collections]
            output: ./output-dir
            emx2:
              endpoint: https://emx2.example.org
              token: my-token
            steps:
              - extract: { url: https://fdp.example.org, crawl: fdp, strict: false }
              - preprocessing: [temporal, stage-csvw]
              - transform
              - postprocessing: [resolve-missing-pk]
              - upload
            """);

    assertEquals("catalogue", definition.schema());
    assertEquals(List.of("Catalogues", "Collections"), definition.tables());
    assertEquals("./output-dir", definition.output());
    assertEquals(new Emx2Settings("https://emx2.example.org", "my-token"), definition.emx2());
    assertEquals(URI.create("https://fdp.example.org"), definition.extract().url());
    assertFalse(definition.extract().strict());
    assertEquals(List.of(new TemporalSpec(), new StageCsvwSpec()), definition.preProcessors());
    assertEquals(List.of(new ResolveMissingPkSpec()), definition.postProcessors());
    assertTrue(definition.upload());
  }

  @Test
  void shouldLeaveOutOptionalPartsWhenNotListed() {
    PipelineDefinition definition = PipelineFile.parse(withSteps(""));

    assertNull(definition.output());
    assertEquals(List.of(), definition.preProcessors());
    assertEquals(List.of(), definition.postProcessors());
    assertFalse(definition.upload());
  }

  @Test
  void shouldIgnoreOrderOfStages() {
    PipelineDefinition definition =
        PipelineFile.parse(
            steps(
                """
                - upload
                - postprocessing: [resolve-missing-pk]
                - transform
                - extract: { url: https://fdp.example.org }
                """));

    assertTrue(definition.upload());
    assertEquals(List.of(new ResolveMissingPkSpec()), definition.postProcessors());
  }

  @Test
  void shouldAcceptEmptyOptionsForStagesWithoutOptions() {
    PipelineDefinition definition =
        PipelineFile.parse(steps(EXTRACT + "- transform: {}\n- upload: {}\n"));

    assertTrue(definition.upload());
  }

  @Test
  void shouldAcceptPreAndPostProcessingWithoutEntries() {
    PipelineDefinition definition =
        PipelineFile.parse(withSteps("- preprocessing\n- postprocessing: []\n"));

    assertEquals(List.of(), definition.preProcessors());
    assertEquals(List.of(), definition.postProcessors());
  }

  @Test
  void shouldCreateWorkingPostProcessorsInListedOrder() {
    List<PostProcessor> postProcessors =
        PipelineFile.parse(
                withSteps(
                    """
                    - postprocessing:
                        - coalesce-field: { table: Collections, field: id, derive-from: [acronym] }
                        - coalesce-field: { table: Collections, field: label, derive-from: [id] }
                    """))
            .createPostProcessors(CONTEXT);

    InMemoryTableStore store = storeWithRow(new Row("acronym", "BX"));
    postProcessors.forEach(postProcessor -> postProcessor.process(store));

    assertEquals("BX", firstRow(store).getString("label"));
  }

  @Test
  void shouldReadFromPath(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("pipeline.yaml");
    Files.writeString(file, withSteps(""));

    assertEquals("catalogue", PipelineFile.read(file).schema());
  }

  @Test
  void shouldRejectMissingSchema() {
    assertInvalid(withSteps("").replace("schema: catalogue\n", ""), "schema is required");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "tables: []\n"})
  void shouldRejectMissingTables(String tables) {
    assertInvalid(
        withSteps("").replace("tables: [Collections]\n", tables),
        "tables must list at least one table");
  }

  @Test
  void shouldRejectMissingEmx2() {
    assertInvalid(
        """
        schema: catalogue
        tables: [Collections]
        steps: []
        """,
        "emx2 is required");
  }

  @ParameterizedTest
  @ValueSource(strings = {"  endpoint: http://localhost:8080\n", "  token: token\n"})
  void shouldRejectIncompleteEmx2(String missingLine) {
    assertInvalid(withSteps("").replace(missingLine, ""), "emx2 requires endpoint and token");
  }

  @Test
  void shouldRejectUnknownTopLevelField() {
    assertInvalid("stepz: []\n" + withSteps(""), "stepz");
  }

  @Test
  void shouldRejectUnknownEmx2Field() {
    assertInvalid(
        withSteps("").replace("  token: token\n", "  token: token\n  user: me\n"), "user");
  }

  @Test
  void shouldRejectMissingSteps() {
    assertInvalid(SETTINGS, "'steps' must be a list");
  }

  @Test
  void shouldRejectMissingExtractStage() {
    assertInvalid(steps(TRANSFORM), "the extract stage is required");
  }

  @Test
  void shouldRejectMissingTransformStage() {
    assertInvalid(steps(EXTRACT), "the transform stage is required");
  }

  @Test
  void shouldRejectOptionsOnTransformStage() {
    assertInvalid(
        steps(EXTRACT + "- transform: { fast: true }\n"), "stage 'transform' takes no options");
  }

  @Test
  void shouldRejectOptionsOnUploadStage() {
    assertInvalid(withSteps("- upload: { fast: true }\n"), "stage 'upload' takes no options");
  }

  @Test
  void shouldRejectUnknownStage() {
    assertInvalid(withSteps("- postprocesing: []\n"), "unknown stage 'postprocesing'");
  }

  @Test
  void shouldRejectUnknownBareStage() {
    assertInvalid(withSteps("- uplod\n"), "unknown stage 'uplod'");
  }

  @Test
  void shouldRejectStageListedMoreThanOnce() {
    assertInvalid(withSteps("- transform\n"), "stage 'transform' is listed more than once");
  }

  @Test
  void shouldRejectStepNamingMoreThanOneStage() {
    assertInvalid(steps(EXTRACT + "- { transform: {}, upload: {} }\n"), "exactly one stage");
  }

  @Test
  void shouldRejectUnknownPostProcessor() {
    assertInvalid(
        withSteps(
            """
            - postprocessing:
                - coalesce-feild: { table: Collections, field: id, derive-from: [name] }
            """),
        "coalesce-feild");
  }

  @Test
  void shouldRejectUnknownBarePostProcessor() {
    assertInvalid(withSteps("- postprocessing: [resolve-ontologys]\n"), "resolve-ontologys");
  }

  private static InMemoryTableStore storeWithRow(Row row) {
    InMemoryTableStore store = new InMemoryTableStore();
    store.writeTable(TABLE_NAME, new ArrayList<>(row.getColumnNames()), List.of(row));
    return store;
  }

  private static Row firstRow(InMemoryTableStore store) {
    return StreamSupport.stream(store.readTable(TABLE_NAME).spliterator(), false)
        .findFirst()
        .orElseThrow();
  }
}
