package org.molgenis.emx2.fairmapper;

import static org.molgenis.emx2.datamodels.util.CompareTools.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairmapper.postprocessing.OntologyResolver;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class OntologyResolverTest {

  private static final String SCHEMA_NAME = OntologyResolverTest.class.getSimpleName();

  private TableMetadata figures;
  private OntologyResolver resolver;

  @BeforeEach
  void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    SchemaMetadata schema = database.dropCreateSchema(SCHEMA_NAME).getMetadata();

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

    figures =
        schema.create(
            new TableMetadata("figures")
                .add(
                    Column.column("name").setType(ColumnType.STRING).setPkey(),
                    Column.column("shape").setType(ColumnType.ONTOLOGY).setRefTable("shapes"),
                    Column.column("colors")
                        .setType(ColumnType.ONTOLOGY_ARRAY)
                        .setRefTable("colors")));

    resolver = new OntologyResolver();
  }

  @Test
  void shouldReplaceOntologyTermURIWithName() {
    Row row = new Row("name", "foo", "shape", "http://www.example.org/shapes#square");
    resolver.resolve(figures.getColumn("shape"), row);
    assertEquals(row, new Row("name", "foo", "shape", "square"));
  }

  @Test
  void onlyReplaceSelectedOntologyTermURI() {
    Row row =
        new Row(
            "name",
            "foo",
            "shape",
            "http://www.example.org/shapes#triangle",
            "colors",
            "http://www.example.org/colors#green,http://www.example.org/colors#blue");
    resolver.resolve(figures.getColumn("shape"), row);
    assertEquals(
        row,
        new Row(
            "name",
            "foo",
            "shape",
            "triangle",
            "colors",
            "http://www.example.org/colors#green,http://www.example.org/colors#blue"));
  }

  @Test
  void shouldClearNonExistingOntology() {
    Row row = new Row("name", "foo", "shape", "http://www.example.org/shape#hexagon");
    resolver.resolve(figures.getColumn("shape"), row);
    assertEquals(row, new Row("name", "foo"));
  }

  @Test
  void shouldReplaceOntologyArray() {
    Row row =
        new Row(
            "name",
            "foo",
            "colors",
            "http://www.example.org/colors#green,http://www.example.org/colors#blue");
    resolver.resolve(figures.getColumn("colors"), row);
    assertEquals(row, new Row("name", "foo", "colors", "green,blue"));
  }

  @Test
  void shouldClearNonExistingOntologyArray() {
    Row row =
        new Row(
            "name",
            "foo",
            "colors",
            "http://www.example.org/colors#green,http://www.example.org/colors#magenta");
    resolver.resolve(figures.getColumn("colors"), row);
    assertEquals(row, new Row("name", "foo"));
  }

  @Test
  void shouldClearOntologyArrayWhenNoSemanticTermURIForExistingOntology() {
    Row row =
        new Row(
            "name",
            "foo",
            "colors",
            "http://www.example.org/colors#green,http://www.example.org/colors#purple");
    resolver.resolve(figures.getColumn("colors"), row);
    assertEquals(row, new Row("name", "foo"));
  }
}
