package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.harvester.util.HarvestingTestSchema;

class Poc {

  private SailRepositoryConnection conn;
  private SchemaMetadata schema;

  @BeforeEach
  void setUp() throws IOException {
    schema = HarvestingTestSchema.create();
    SailRepository repository = new SailRepository(new MemoryStore());
    conn = repository.getConnection();
    try (InputStream inputStream = readTtl("dataset.ttl")) {
      conn.add(inputStream, RDFFormat.TURTLE);
    }
  }

  @AfterEach
  void tearDown() {
    conn.close();
  }

  @Test
  void queryCatalog() {
    TableSparqlQuery query = new TableSparqlQuery(schema, "Resources");
    query.build();
    System.out.println(query.asString());
  }

  private InputStream readTtl(String path) {
    return TableSparqlQueryTest.class.getResourceAsStream(path);
  }
}
