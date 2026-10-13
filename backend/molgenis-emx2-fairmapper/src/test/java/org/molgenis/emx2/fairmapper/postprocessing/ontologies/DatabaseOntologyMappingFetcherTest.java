package org.molgenis.emx2.fairmapper.postprocessing.ontologies;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlColumnExecutor;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class DatabaseOntologyMappingFetcherTest {

  private static final String SCHEMA_NAME =
      DatabaseOntologyMappingFetcherTest.class.getSimpleName();

  private final Database database = TestDatabaseFactory.getTestDatabase();
  private Table colors;
  private DatabaseOntologyMappingFetcher mappingFetcher;

  @BeforeEach
  void setUp() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    colors =
        schema.create(SqlColumnExecutor.getOntologyTableDefinition("colors", Map.of(), Map.of()));
    mappingFetcher = new DatabaseOntologyMappingFetcher(database);

    addColor("red", "https://dbpedia.org/page/red");
    addColor("white", "https://dbpedia.org/page/white");
    addColor("blue", "https://dbpedia.org/page/blue");
  }

  @Test
  void shouldGetOntologyMapping() {
    Map<String, String> actual = mappingFetcher.getMapping(SCHEMA_NAME, "colors");
    Map<String, String> expected =
        Map.of(
            "https://dbpedia.org/page/red", "red",
            "https://dbpedia.org/page/white", "white",
            "https://dbpedia.org/page/blue", "blue");
    assertEquals(expected, actual);
  }

  @Test
  void givenNonExistingSchemaName_thenEmptyMapping() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class, () -> mappingFetcher.getMapping("non-existing", "colors"));
    assertEquals("No schema found with name: non-existing", exception.getMessage());
  }

  @Test
  void givenNonExistingTableName_thenEmptyMapping() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class, () -> mappingFetcher.getMapping(SCHEMA_NAME, "non-existing"));
    assertEquals(
        "No table with name: non-existing found for schema DatabaseOntologyMappingFetcherTest",
        exception.getMessage());
  }

  private void addColor(String name, String semanticTermURI) {
    colors.insert(Row.row("name", name, "ontologyTermURI", semanticTermURI));
  }
}
