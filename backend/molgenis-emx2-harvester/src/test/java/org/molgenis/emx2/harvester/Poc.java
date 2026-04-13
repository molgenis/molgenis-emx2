package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.InputStream;
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
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class Poc {

  private Database database;
  private Schema schema;
  private SailRepository repository;
  private SailRepositoryConnection conn;

  @BeforeEach
  void setUp() throws IOException {
    database = TestDatabaseFactory.getTestDatabase();
    schema = database.getSchema("catalogue");
    repository = new SailRepository(new MemoryStore());
    conn = repository.getConnection();
    try (InputStream inputStream = readTtl("catalogue.ttl")) {
      conn.add(inputStream, RDFFormat.TURTLE);
    }
  }

  @AfterEach
  void tearDown() {
    conn.close();
  }

  @Test
  void queryCatalog() {
    TableSparqlQuery query = new TableSparqlQuery(schema.getMetadata(), "Resources");
    query.build();
    System.out.println(query.asString());
    TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query.asString());
    //    tupleQuery.evaluate().stream().forEach(System.out::println);
  }

  private InputStream readTtl(String path) {
    return TableSparqlQueryTest.class.getResourceAsStream(path);
  }
}
