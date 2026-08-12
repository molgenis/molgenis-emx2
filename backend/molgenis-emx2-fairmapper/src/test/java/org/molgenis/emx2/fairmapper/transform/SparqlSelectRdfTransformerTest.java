package org.molgenis.emx2.fairmapper.transform;

import static org.eclipse.rdf4j.model.util.Statements.statement;
import static org.eclipse.rdf4j.model.util.Values.iri;
import static org.eclipse.rdf4j.model.util.Values.literal;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.SemanticTestUtils.toSemantic;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.*;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SparqlSelectRdfTransformerTest {

  public static final IRI SUBJECT = iri("https://example.com/bob");
  private Database database;
  private SchemaMetadata schema;

  @BeforeEach
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    String schemaName = SparqlSelectRdfTransformerTest.class.getSimpleName();
    schema = database.dropCreateSchema(schemaName).getMetadata();
  }

  @Test
  void givenUnknownTable_thenThrow() {
    TableQueryGenerator generator = new TableQueryGenerator();
    List<String> tables = List.of("unknown-1", "unknown-2");
    MolgenisException exception =
        assertThrows(
            MolgenisException.class,
            () -> new SparqlSelectRdfTransformer(generator, schema, tables));
    assertEquals(
        "Unknown table(s) provided to transformer: unknown-1, unknown-2 for schema: SparqlSelectRdfTransformerTest",
        exception.getMessage());
  }

  @Test
  void shouldSplitArrayValues() {
    schema.create(
        TableMetadata.table("splitArrays_string")
            .add(
                Column.column("id").setType(ColumnType.INT).setPkey(),
                Column.column("names")
                    .setType(ColumnType.STRING_ARRAY)
                    .setSemantics(FOAF.FIRST_NAME.stringValue())));

    SailRepository repository =
        createRepositoryWithStatements(
            statement(SUBJECT, FOAF.FIRST_NAME, literal("foo"), null),
            statement(SUBJECT, FOAF.FIRST_NAME, literal("bar"), null));

    TableStore transform =
        new SparqlSelectRdfTransformer(
                new TableQueryGenerator(), schema, List.of("splitArrays_string"))
            .transform(repository);

    Iterator<Row> iterator = transform.readTable("splitArrays_string").iterator();
    CompareTools.assertEquals(
        iterator.next(),
        Row.row("_subject_", SUBJECT.stringValue(), "names", new String[] {"foo", "bar"}));
    assertFalse(iterator.hasNext());
  }

  @Test
  void shouldHandleNonStringValues() {
    schema.create(
        TableMetadata.table("splitArrays_integer")
            .add(
                Column.column("id").setType(ColumnType.INT).setPkey(),
                Column.column("numbers")
                    .setType(ColumnType.INT_ARRAY)
                    .setSemantics(FOAF.FIRST_NAME.stringValue())));

    SailRepository repository =
        createRepositoryWithStatements(
            statement(SUBJECT, FOAF.FIRST_NAME, literal(1), null),
            statement(SUBJECT, FOAF.FIRST_NAME, literal(2), null));

    TableStore transform =
        new SparqlSelectRdfTransformer(
                new TableQueryGenerator(), schema, List.of("splitArrays_integer"))
            .transform(repository);

    Iterator<Row> iterator = transform.readTable("splitArrays_integer").iterator();
    Row next = iterator.next();
    CompareTools.assertEquals(
        next, Row.row("_subject_", SUBJECT.stringValue(), "numbers", new String[] {"1", "2"}));
    assertArrayEquals(new Integer[] {1, 2}, next.getIntegerArray("numbers"));
    assertFalse(iterator.hasNext());
  }

  @Test
  void shouldSplitRefArraySubjects() {
    schema.create(TableMetadata.table("splitRefArrays_tag").add(Column.column("name").setPkey()));
    schema.create(
        TableMetadata.table("splitRefArrays_owner")
            .add(
                Column.column("id").setType(ColumnType.INT).setPkey(),
                Column.column("tags")
                    .setType(ColumnType.REF_ARRAY)
                    .setRefTable("splitRefArrays_tag")
                    .setSemantics(FOAF.KNOWS.stringValue())));

    IRI tag1 = iri("https://example.com/tag/1");
    IRI tag2 = iri("https://example.com/tag/2");

    SailRepository repository =
        createRepositoryWithStatements(
            statement(SUBJECT, FOAF.KNOWS, tag1, null), statement(SUBJECT, FOAF.KNOWS, tag2, null));

    TableStore transform =
        new SparqlSelectRdfTransformer(
                new TableQueryGenerator(), schema, List.of("splitRefArrays_owner"))
            .transform(repository);

    Iterator<Row> iterator = transform.readTable("splitRefArrays_owner").iterator();
    Row row = iterator.next();
    assertArrayEquals(
        new String[] {tag1.stringValue(), tag2.stringValue()}, row.getStringArray("_subject_tags"));
    assertFalse(iterator.hasNext());
  }

  @Test
  void givenTtlData_thenQueryTable() throws IOException {
    String schemaName = SparqlSelectRdfTransformerTest.class.getSimpleName() + "_petstore";
    database.dropSchemaIfExists(schemaName);
    DataModels.Profile.PET_STORE
        .getImportTask(database, schemaName, "RDF data transformation test", false)
        .run();

    SparqlSelectRdfTransformer transformer =
        new SparqlSelectRdfTransformer(
            new TableQueryGenerator(),
            database.getSchema(schemaName).getMetadata(),
            List.of("Pet"));

    SailRepository repository = readPetStoreTtl();
    TableStore store = transformer.transform(repository);

    StringWriter writer = new StringWriter();
    CsvTableWriter.write(
        store.readTable("Pet"), List.of("name", "status", "weight", "tags"), writer, ',');

    String expected = readPetsCsv();
    assertEquals(expected, writer.toString());
  }

  private SailRepository createRepositoryWithStatements(Statement... statements) {
    SailRepository repository = new SailRepository(new MemoryStore());

    try (SailRepositoryConnection connection = repository.getConnection()) {
      for (Statement statement : statements) {
        connection.add(statement);
      }
      connection.commit();
    }

    return repository;
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
      SparqlSelectRdfTransformer transformer =
          new SparqlSelectRdfTransformer(
              new TableQueryGenerator(), setupSchema(), List.of("testTable"));
      TableStore transform = transformer.transform(repository);
      testData = transform.readTable("testTable").iterator().next();
    }

    private SchemaMetadata setupSchema() {
      return new SchemaMetadata(getClass().getSimpleName() + "_mapColumnNames")
          .create(
              TableMetadata.table(
                  "testTable",
                  Column.column("first name").setSemantics(toSemantic(FIRST_NAME_SEMANTIC)),
                  Column.column("last_name").setSemantics(toSemantic(LAST_NAME_SEMANTIC))))
          .getSchema();
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
      assertEquals(SUBJECT.stringValue(), testData.getString("_subject_"));
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
}
