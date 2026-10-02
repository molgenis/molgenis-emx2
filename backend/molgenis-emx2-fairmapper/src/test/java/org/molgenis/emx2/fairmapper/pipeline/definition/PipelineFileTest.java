package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

class PipelineFileTest {

  private static final String TABLE_NAME = "Collections";
  private static final PipelineContext CONTEXT = new PipelineContext(null, null);

  @Test
  void shouldCreateCoalesceFieldPostProcessor() {
    List<PostProcessor> postProcessors =
        postProcessorsOf(
            """
            steps:
              - postprocessing:
                  - coalesce-field:
                      table: Collections
                      field: id
                      derive-from: [acronym, name]
            """);

    InMemoryTableStore store = storeWithRow(new Row("name", "Biobank X"));
    postProcessors.forEach(postProcessor -> postProcessor.process(store));

    assertEquals("Biobank X", firstRow(store).getString("id"));
  }

  @Test
  void shouldKeepListedOrderOfPostProcessors() {
    List<PostProcessor> postProcessors =
        postProcessorsOf(
            """
            steps:
              - postprocessing:
                  - coalesce-field: { table: Collections, field: id, derive-from: [acronym] }
                  - coalesce-field: { table: Collections, field: label, derive-from: [id] }
            """);

    InMemoryTableStore store = storeWithRow(new Row("acronym", "BX"));
    postProcessors.forEach(postProcessor -> postProcessor.process(store));

    assertEquals("BX", firstRow(store).getString("label"));
  }

  @Test
  void shouldReadFromPath(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("pipeline.yaml");
    Files.writeString(
        file,
        """
        steps:
          - postprocessing:
              - coalesce-field: { table: Collections, field: id, derive-from: [name] }
        """);

    assertEquals(1, PipelineFile.read(file).createPostProcessors(CONTEXT).size());
  }

  @Test
  void shouldRejectUnknownPostProcessor() {
    assertInvalid(
        """
        steps:
          - postprocessing:
              - coalesce-feild: { table: Collections, field: id, derive-from: [name] }
        """,
        "coalesce-feild");
  }

  @Test
  void shouldRejectUnknownStage() {
    assertInvalid(
        """
        steps:
          - postprocesing: []
        """,
        "unknown stage 'postprocesing'");
  }

  @Test
  void shouldRejectStageListedMoreThanOnce() {
    assertInvalid(
        """
        steps:
          - postprocessing: []
          - postprocessing: []
        """,
        "stage 'postprocessing' is listed more than once");
  }

  @Test
  void shouldRejectUnknownTopLevelField() {
    assertInvalid(
        """
        stepz: []
        """,
        "unknown field 'stepz'");
  }

  private static List<PostProcessor> postProcessorsOf(String yaml) {
    return PipelineFile.parse(yaml).createPostProcessors(CONTEXT);
  }

  private static void assertInvalid(String yaml, String expectedMessagePart) {
    MolgenisException exception =
        assertThrows(MolgenisException.class, () -> PipelineFile.parse(yaml));
    assertTrue(
        exception.getMessage().contains(expectedMessagePart),
        () -> "Expected '" + expectedMessagePart + "' in: " + exception.getMessage());
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
