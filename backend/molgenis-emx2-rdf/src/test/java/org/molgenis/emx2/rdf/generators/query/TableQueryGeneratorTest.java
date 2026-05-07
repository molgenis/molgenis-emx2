package org.molgenis.emx2.rdf.generators.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.*;
import java.util.List;
import java.util.Objects;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TableQueryGeneratorTest {

  private static final ClassLoader LOADER = TableQueryGeneratorTest.class.getClassLoader();
  private Schema schema;
  private TableMetadata order;
  private Database database;

  @BeforeEach
  void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(getClass().getSimpleName());
  }

  @BeforeEach
  void setup() {
    schema.create(productTableWithSemantics("xsd:name"));
    order = schema.create(orderTable(true)).getMetadata();
  }

  @Test
  void shouldPropagatePlainColumns() {
    TableMetadata table =
        TableMetadata.table(
            "Propogation",
            Column.column("name").setType(ColumnType.STRING).setPkey().setSemantics("xsd:name"));

    SelectQuery generate = new TableQueryGenerator().generate(table);
    assertEquals(
        removePrefixesFromQuery(generate.getQueryString()),
        """
      SELECT ?Propogation ?name
      WHERE { ?Propogation xsd:name ?name . }
      GROUP BY ?Propogation ?name
      """);
  }

  @Test
  void shouldPropagateArrays() {
    TableMetadata table =
        TableMetadata.table(
            "Propogation",
            Column.column("names").setType(ColumnType.STRING_ARRAY).setSemantics("xsd:name"));

    SelectQuery generate = new TableQueryGenerator().generate(table);
    assertEquals(
        removePrefixesFromQuery(generate.getQueryString()),
        """
          SELECT ?Propogation ( GROUP_CONCAT( DISTINCT STR( ?names_single ) ; SEPARATOR = ',' ) AS ?names )
          WHERE { OPTIONAL { ?Propogation xsd:name ?names_single . } }
          GROUP BY ?Propogation
          """);
  }

  @Test
  void shouldPropagateReferences() {
    SelectQuery query = new TableQueryGenerator().generate(order);

    assertEquals(
        """
        SELECT ?Order ?id ?product_name
        WHERE { ?Order xsd:id ?id .
        ?Order xsd:product ?product .
        ?product xsd:name ?product_name . }
        GROUP BY ?Order ?id ?product_name
        """,
        removePrefixesFromQuery(query.getQueryString()));
  }

  private String removePrefixesFromQuery(String query) {
    return query.replaceAll("PREFIX .*\n", "");
  }

  @Test
  void shouldSetUpPrefixes() {
    SelectQuery select = Queries.SELECT();
    DefaultNamespace.streamAll().forEach(select::prefix);
    String prefixes = select.getQueryString().replace("SELECT * \n" + "WHERE {}", "").trim();

    TableQueryGenerator generator = new TableQueryGenerator();
    String actualQuery = generator.generate(order).getQueryString();
    assertTrue(actualQuery.startsWith(prefixes));
  }

  @Test
  void shouldQueryPets() throws IOException {
    String schemaName = getClass().getSimpleName() + "_pet";
    database.dropSchemaIfExists(schemaName);
    DataModels.Profile.PET_STORE
        .getImportTask(database, schemaName, "Test querying pets", false)
        .run();

    Schema petSchema = database.getSchema(schemaName);
    Table pet = petSchema.getTable("Pet");
    SelectQuery generate = new TableQueryGenerator().generate(pet.getMetadata());

    SailRepository repository = setupRepositoryFromFile("queries/pets.ttl");
    TupleQuery query =
        repository
            .getConnection()
            .prepareTupleQuery(QueryLanguage.SPARQL, generate.getQueryString());

    StringWriter writer = new StringWriter();
    query.evaluate(new SPARQLResultsCSVWriter(writer));

    byte[] fileBytes =
        Objects.requireNonNull(LOADER.getResourceAsStream("queries/expected_pets.csv"))
            .readAllBytes();
    List<String> expectedLines = new String(fileBytes).lines().toList();
    List<String> actualLines = writer.toString().lines().toList();
    assertEquals(expectedLines, actualLines);
  }

  private SailRepository setupRepositoryFromFile(String fileName) {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection conn = repository.getConnection();
        InputStream fileInputStream = LOADER.getResourceAsStream(fileName)) {
      conn.add(fileInputStream, RDFFormat.TURTLE);
      conn.commit();
    } catch (IOException e) {
      throw new AssertionError("Unable to read RDF from file: " + fileName, e);
    }
    return repository;
  }

  private TableMetadata orderTable(boolean productRequired) {
    return TableMetadata.table(
        "Order",
        Column.column("id").setPkey().setType(ColumnType.STRING).setSemantics("xsd:id"),
        Column.column("product")
            .setType(ColumnType.REF)
            .setRefTable("Product")
            .setRequired(productRequired)
            .setSemantics("xsd:product"));
  }

  private TableMetadata productTableWithSemantics(String... semantics) {
    return TableMetadata.table(
        "Product",
        Column.column("name").setType(ColumnType.STRING).setPkey().setSemantics(semantics),
        Column.column("price").setType(ColumnType.DECIMAL));
  }
}
