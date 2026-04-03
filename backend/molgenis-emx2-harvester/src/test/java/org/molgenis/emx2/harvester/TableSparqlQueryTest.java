package org.molgenis.emx2.harvester;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.rdf.DefaultNamespace;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TableSparqlQueryTest {

  private Database database;
  private Schema schema;
  private SailRepository repository;
  private SailRepositoryConnection conn;

  @BeforeEach
  void setUp() throws IOException {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.getSchema("pet store");
    repository = new SailRepository(new MemoryStore());
    conn = repository.getConnection();
    try (InputStream inputStream = readTtl("pet-store.ttl")) {
      conn.add(inputStream, RDFFormat.TURTLE);
    }
  }

  @AfterEach
  void tearDown() {
    conn.close();
  }

  @Test
  void printTest() {
    TableSparqlQuery query = new TableSparqlQuery(schema.getMetadata(), "Pet");
    query.build();
    System.out.println(query.asString());
  }

  @Test
  void shouldReadPets() {
    TableSparqlQuery query = new TableSparqlQuery(schema.getMetadata(), "Pet");
    query.build();
    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.asString());
    tupleQuery.evaluate().stream().forEach(System.out::println);
  }

  @Test
  void givenPerson_thenDontAddPointBrackets() {
    TableSparqlQuery query =
        new TableSparqlQuery(
            schema.getMetadata(), "User", Stream.of(DefaultNamespace.FOAF.getNamespace()));

    query.build();
    assertEquals(
        """
                PREFIX foaf: <http://xmlns.com/foaf/0.1/>
                SELECT ?username ?firstName ?lastName ?picture ?email ?phone ?pets
                WHERE { ?User a foaf:Person ;
                    foaf:accountName ?username .
                OPTIONAL { ?User foaf:firstName ?firstName . }
                OPTIONAL { ?User foaf:lastName ?lastName . }
                OPTIONAL { ?User foaf:img ?picture . }
                OPTIONAL { ?User foaf:mbox ?email . }
                OPTIONAL { ?User foaf:phone ?phone . }
                OPTIONAL { ?pets http://example.com/petstore#hasPets ?nameRef .
                ?nameRef http://example.com/petstore#hasName ?name . } }
                """,
        query.asString());
  }

  private InputStream readTtl(String path) {
    return TableSparqlQueryTest.class.getResourceAsStream(path);
  }
}
