package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class TableSparqlQueryTest {

  private static final String SCHEMA_NAME = TableSparqlQueryTest.class.getSimpleName();

  private static Schema schema;
  private SailRepositoryConnection conn;

  @BeforeAll
  static void createSchema() {
    Database database = TestDatabaseFactory.getTestDatabase();
    database.dropSchemaIfExists(SCHEMA_NAME);
    DataModels.Profile.PET_STORE
        .getImportTask(database, SCHEMA_NAME, "SparQL query generation test", true)
        .run();
    schema = database.getSchema(SCHEMA_NAME);
    schema.getTable("Pet").getMetadata().setSemantics("http://example.com/pet");
  }

  @BeforeEach
  void setUp() throws IOException {
    SailRepository repository = new SailRepository(new MemoryStore());
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

  private InputStream readTtl(String path) {
    return TableSparqlQueryTest.class.getResourceAsStream(path);
  }
}
