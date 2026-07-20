package org.molgenis.emx2.fairmapper.postprocessing;

import static org.molgenis.emx2.datamodels.util.CompareTools.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class OntologyResolverTest {

  private static final String SCHEMA_NAME = OntologyResolverTest.class.getSimpleName();
  private static final String FIGURES_TABLE = "figures";

  private OntologyResolver resolver;
  private SchemaMetadata schema;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(SCHEMA_NAME).getMetadata();

    TableMetadata shapes = new TableMetadata("shapes").setTableType(TableType.ONTOLOGIES);
    schema
        .create(shapes)
        .getTable()
        .insert(
            Row.row("name", "square", "ontologyTermURI", "http://www.example.org/shapes#square"),
            Row.row("name", "circle", "ontologyTermURI", "http://www.example.org/shapes#circle"),
            Row.row(
                "name", "triangle", "ontologyTermURI", "http://www.example.org/shapes#triangle"),
            Row.row("name", "hexagon"));

    TableMetadata colors = new TableMetadata("colors").setTableType(TableType.ONTOLOGIES);
    schema
        .create(colors)
        .getTable()
        .insert(
            Row.row("name", "red", "ontologyTermURI", "http://www.example.org/colors#red"),
            Row.row("name", "green", "ontologyTermURI", "http://www.example.org/colors#green"),
            Row.row("name", "blue", "ontologyTermURI", "http://www.example.org/colors#blue"),
            Row.row("name", "purple"));

    schema.create(
        new TableMetadata(FIGURES_TABLE)
            .add(
                Column.column("name").setType(ColumnType.STRING).setPkey(),
                Column.column("shape").setType(ColumnType.ONTOLOGY).setRefTable("shapes"),
                Column.column("colors").setType(ColumnType.ONTOLOGY_ARRAY).setRefTable("colors")));

    resolver = new OntologyResolver(schema);
  }

  @Test
  void shouldReplaceOntologyTermURIWithName() {
    TableStore tableStore =
        getTableStoreWithRows(
            new Row("name", "foo", "shape", "http://www.example.org/shapes#square"));
    resolver.process(tableStore);
    assertTableStoreHasFiguresTableWithRows(tableStore, new Row("name", "foo", "shape", "square"));
  }

  @Test
  void givenRow_whenOntologyDoesNotExist_thenClearCell() {
    TableStore tableStore =
        getTableStoreWithRows(
            new Row("name", "foo", "shape", "http://www.example.org/shape#hexagon"));
    resolver.process(tableStore);
    assertTableStoreHasFiguresTableWithRows(tableStore, new Row("name", "foo"));
  }

  @Test
  void givenRow_whenMultipleCellsWithOntologies_thenReplaceAll() {
    TableStore tableStore =
        getTableStoreWithRows(
            new Row(
                "name",
                "foo",
                "shape",
                "http://www.example.org/shapes#triangle",
                "colors",
                "http://www.example.org/colors#green,http://www.example.org/colors#blue"));
    resolver.process(tableStore);
    assertTableStoreHasFiguresTableWithRows(
        tableStore, new Row("name", "foo", "shape", "triangle", "colors", "green,blue"));
  }

  @Test
  void givenRow_whenOntologyArray_thenReplaceAllValues() {
    TableStore tableStore =
        getTableStoreWithRows(
            new Row(
                "name",
                "foo",
                "colors",
                "http://www.example.org/colors#green,http://www.example.org/colors#blue"));
    resolver.process(tableStore);
    assertTableStoreHasFiguresTableWithRows(
        tableStore, new Row("name", "foo", "colors", "green,blue"));
  }

  @Test
  void givenRow_whenOntologyArrayWithNonExistingValue_thenClearCell() {
    TableStore tableStore =
        getTableStoreWithRows(
            new Row(
                "name",
                "foo",
                "colors",
                "http://www.example.org/colors#green,http://www.example.org/colors#magenta"));
    resolver.process(tableStore);
    assertTableStoreHasFiguresTableWithRows(tableStore, new Row("name", "foo"));
  }

  @Test
  void givenMultipleTables_thenProcessAll() {
    schema.create(
        new TableMetadata("pictures")
            .add(
                Column.column("name").setType(ColumnType.STRING).setPkey(),
                Column.column("shape").setType(ColumnType.ONTOLOGY).setRefTable("shapes"),
                Column.column("color").setType(ColumnType.ONTOLOGY).setRefTable("colors")));

    TableStore tableStore =
        getTableStoreWithRows(
            new Row(
                "name",
                "foo",
                "colors",
                "http://www.example.org/colors#green,http://www.example.org/colors#red"));

    tableStore.writeTable(
        "pictures",
        List.of("name", "shape", "color"),
        List.of(
            new Row(
                "name",
                "Lava Ball",
                "shape",
                "http://www.example.org/shapes#circle",
                "color",
                "http://www.example.org/colors#red")));

    resolver.process(tableStore);

    assertTableStoreHasTableWithRows(
        tableStore, FIGURES_TABLE, new Row("name", "foo", "colors", "green,red"));
    assertTableStoreHasTableWithRows(
        tableStore, "pictures", new Row("name", "Lava Ball", "shape", "circle", "color", "red"));
  }

  private TableStore getTableStoreWithRows(Row... rows) {
    InMemoryTableStore store = new InMemoryTableStore();
    if (rows.length == 0) {
      store.writeTable(FIGURES_TABLE, List.of(), List.of());
    } else {
      store.writeTable(
          FIGURES_TABLE, new ArrayList<>(rows[0].getColumnNames()), Arrays.asList(rows));
    }

    return store;
  }

  private void assertTableStoreHasFiguresTableWithRows(TableStore tableStore, Row... rows) {
    assertTableStoreHasTableWithRows(tableStore, FIGURES_TABLE, rows);
  }

  private void assertTableStoreHasTableWithRows(TableStore tableStore, String table, Row... rows) {
    List<Row> actual =
        StreamSupport.stream(tableStore.readTable(table).spliterator(), false).toList();
    List<Row> expected = Arrays.stream(rows).toList();
    assertEquals(actual, expected);
  }
}
