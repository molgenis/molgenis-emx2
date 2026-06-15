package org.molgenis.emx2.fairmapper.transform;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.FileBasedQueryGenerator;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SparqlSelectRdfTransformerTest {

  private Database database;

  @BeforeEach
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void givenUnknownTable_thenThrow() {
    String schemaName = SparqlSelectRdfTransformerTest.class.getSimpleName() + "_unknowntable";
    SchemaMetadata schema = database.dropCreateSchema(schemaName).getMetadata();
    TableQueryGenerator generator = new TableQueryGenerator();
    List<String> tables = List.of("unknown-1", "unknown-2");
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> new SparqlSelectRdfTransformer(generator, schema, tables));
    assertEquals(
        "Unknown table(s) provided to transformer: unknown-1, unknown-2 for schema: SparqlSelectRdfTransformerTest_unknowntable",
        exception.getMessage());
  }

  @Test
  void givenData_thenQueryTable() throws IOException {
    String schemaName = SparqlSelectRdfTransformerTest.class.getSimpleName() + "_petstore";
    database.dropSchemaIfExists(schemaName);
    DataModels.Profile.PET_STORE
        .getImportTask(database, schemaName, "RDF data transformation test", false)
        .run();

    SparqlSelectRdfTransformer transformer =
        new SparqlSelectRdfTransformer(
            new FileBasedQueryGenerator(Map.of("Pet", getQueryFilePath())),
            database.getSchema(schemaName).getMetadata(),
            List.of("Pet"));

    SailRepository repository = readPetStoreTtl();
    TableStore store = transformer.transform(repository);

    StringWriter writer = new StringWriter();
    CsvTableWriter.write(
        store.readTable("Pet"), List.of("Pet", "name", "status", "weight", "tags"), writer, ',');

    String expected = readPetsCsv();
    assertEquals(expected, writer.toString());
  }

  private SailRepository readPetStoreTtl() {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection connection = repository.getConnection()) {
      URL url = SparqlSelectRdfTransformerTest.class.getResource("petstore.ttl");
      connection.add(url, RDFFormat.TURTLE);
      connection.commit();
    } catch (IOException e) {
      fail("Unable to set up SailRepository for petstore.ttl", e);
    }

    return repository;
  }

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  class NameMappingTest {

    private static final IRI SUBJECT = iri("https://example.com/bob");

    private static final IRI FIRST_NAME_SEMANTIC = FOAF.FIRST_NAME;
    private static final IRI LAST_NAME_SEMANTIC = FOAF.LAST_NAME;

    private Row testData;

    @BeforeAll
    void queryTestData() {

      // Set up a SailRepository with a single statement to test against
      SailRepository repository = setupRepository();
      SchemaMetadata schema = setupSchema();

      SparqlSelectRdfTransformer transformer =
          new SparqlSelectRdfTransformer(new TableQueryGenerator(), schema, List.of("testTable"));
      TableStore transform = transformer.transform(repository);
      testData = transform.readTable("testTable").iterator().next();
    }

    private SchemaMetadata setupSchema() {
      SchemaMetadata schema = new SchemaMetadata(getClass().getSimpleName() + "_mapColumnNames");
      schema.create(
          TableMetadata.table(
              "testTable",
              Column.column("first name").setSemantics(FIRST_NAME_SEMANTIC.toString()),
              Column.column("last_name").setSemantics(LAST_NAME_SEMANTIC.toString())));
      return schema;
    }

    private SailRepository setupRepository() {
      SailRepository repository = new SailRepository(new MemoryStore());

      try (SailRepositoryConnection connection = repository.getConnection()) {
        connection.add(statement(SUBJECT, FIRST_NAME_SEMANTIC, literal("Bob"), null));
        connection.add(statement(SUBJECT, LAST_NAME_SEMANTIC, literal("Ross"), null));
        connection.commit();
      }
      return repository;
    }

    @Test
    void shouldIncludeRootIRI() {
      assertEquals(SUBJECT.stringValue(), testData.getString("_subject"));
    }

    @Test
    void shouldMapSpaces() {
      assertEquals("Bob", testData.getString("first name"));
    }

    @Test
    void shouldMapUnderScores() {
      assertEquals("Ross", testData.getString("last_name"));
    }
  }

  private static String readPetsCsv() throws IOException {
    return new String(
        Objects.requireNonNull(SparqlSelectRdfTransformerTest.class.getResourceAsStream("pets.csv"))
            .readAllBytes());
  }

  private static Path getQueryFilePath() {
    return Paths.get(
        Objects.requireNonNull(SparqlSelectRdfTransformerTest.class.getResource("query.rq"))
            .getPath());
  }
}
