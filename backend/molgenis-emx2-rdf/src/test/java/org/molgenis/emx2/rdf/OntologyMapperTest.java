package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class OntologyMapperTest {
  private static Schema schema;

  @BeforeAll
  static void setUp() {
    Database db = TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema("OntologyMapperTest");

    schema.create(
        table(
            "Resources",
            column("id").setPkey(),
            column("frequency").setType(ColumnType.ONTOLOGY).setRefTable("Frequency")));

    Table frequency = schema.getTable("Frequency");
    frequency.insert(
        new Row()
            .setString("name", "Annually")
            .setString("ontologyTermURI", "http://example.org/frequency/annual")
            .setStringArray(
                "alternativeIds",
                new String[] {"http://publications.europa.eu/resource/authority/frequency/ANNUAL"}),
        new Row()
            .setString("name", "Monthly")
            .setString("ontologyTermURI", "http://example.org/frequency/monthly"));
  }

  @Test
  void resolveByAlternativeIds() {
    OntologyMapper mapper = new OntologyMapper(schema.getTable("Frequency"));
    assertEquals(
        "Annually",
        mapper.resolve("http://publications.europa.eu/resource/authority/frequency/ANNUAL"));
  }

  @Test
  void resolveByOntologyTermUri() {
    OntologyMapper mapper = new OntologyMapper(schema.getTable("Frequency"));
    assertEquals("Monthly", mapper.resolve("http://example.org/frequency/monthly"));
  }

  @Test
  void resolveByNameCaseInsensitive() {
    OntologyMapper mapper = new OntologyMapper(schema.getTable("Frequency"));
    assertEquals("Annually", mapper.resolve("annually"));
    assertEquals("Annually", mapper.resolve("ANNUALLY"));
  }

  @Test
  void noMatchReturnsNull() {
    OntologyMapper mapper = new OntologyMapper(schema.getTable("Frequency"));
    assertNull(mapper.resolve("http://unknown.org/something"));
  }

  @Test
  void cacheIsReused() {
    OntologyMapper mapper = new OntologyMapper(schema.getTable("Frequency"));
    mapper.resolve("http://publications.europa.eu/resource/authority/frequency/ANNUAL");
    mapper.resolve("http://example.org/frequency/monthly");
  }
}
