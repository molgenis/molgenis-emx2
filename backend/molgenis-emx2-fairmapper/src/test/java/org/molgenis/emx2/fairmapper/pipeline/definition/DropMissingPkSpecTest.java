package org.molgenis.emx2.fairmapper.pipeline.definition;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.assertInvalid;
import static org.molgenis.emx2.fairmapper.pipeline.definition.PostProcessorSpecParsing.parse;

import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.TableMetadata;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;

class DropMissingPkSpecTest {

  @Test
  void givenTables_whenParsed_thenMapTables() {
    PostProcessorSpec spec =
        parse(
            """
            drop-missing-pk:
              tables: [Organisations, Collections]
            """);

    assertEquals(new DropMissingPkSpec(List.of("Organisations", "Collections")), spec);
  }

  @Test
  void givenSingleTableValue_whenParsed_thenThrowException() {
    assertInvalid("drop-missing-pk: { tables: Organisations }", "tables");
  }

  @Test
  void givenUnknownArgument_whenParsed_thenThrowException() {
    assertInvalid("drop-missing-pk: { tables: [Organisations], extra: x }", "extra");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"drop-missing-pk: { tables: [] }", "drop-missing-pk: {}", "drop-missing-pk"})
  void givenMissingTables_whenParsed_thenThrowException(String yaml) {
    assertInvalid(yaml, "drop-missing-pk requires tables");
  }

  @Test
  void whenCreated_thenUseSchemaFromContext() {
    SchemaMetadata schema = new SchemaMetadata("test");
    schema.create(
        new TableMetadata("Organisations")
            .add(Column.column("id").setType(ColumnType.STRING).setPkey()));
    PipelineContext context = new PipelineContext(null, schema);

    InMemoryTableStore store = new InMemoryTableStore();
    store.writeTable(
        "Organisations",
        List.of("id", "name"),
        List.of(new Row("id", "org-1", "name", "Kept"), new Row("name", "Dropped")));

    new DropMissingPkSpec(List.of("Organisations")).create(context).process(store);

    List<Row> rows =
        StreamSupport.stream(store.readTable("Organisations").spliterator(), false).toList();
    assertEquals(1, rows.size());
    assertEquals("Kept", rows.getFirst().getString("name"));
  }
}
