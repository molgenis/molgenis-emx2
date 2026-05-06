package org.molgenis.emx2.rdf.generators.query;

import java.io.*;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sparqlbuilder.core.query.SelectQuery;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SparqlQueryingPocTest {

  @Disabled("Used for manual testing purposes")
  @Test
  void testCatalogueQuery() {
    HarvestingTestSchema.create();

    SchemaMetadata schema =
        TestDatabaseFactory.getTestDatabase().getSchema("harvesting").getMetadata();
    TableQueryGenerator generator = new TableQueryGenerator();
    SelectQuery query = generator.generate(schema.getTableMetadata("Resources"));
    System.out.println(query.getQueryString());
  }

  @Disabled("Used for manual testing purposes")
  @Test
  void fileBasedTest() throws IOException {
    String absoluteQueryPath = "";
    String query = new String(new FileInputStream(absoluteQueryPath).readAllBytes());

    String absoluteTtlPath = "";
    SailRepository repository = setupRepositoryFromFile(absoluteTtlPath);

    repository
        .getConnection()
        .prepareTupleQuery(QueryLanguage.SPARQL, query)
        .evaluate(new SPARQLResultsCSVWriter(new FileOutputStream("results.csv")));
  }

  private static SailRepository setupRepositoryFromFile(String fileName) {
    SailRepository repository = new SailRepository(new MemoryStore());
    try (SailRepositoryConnection conn = repository.getConnection();
        InputStream fileInputStream = new FileInputStream(fileName)) {
      conn.add(fileInputStream, RDFFormat.TURTLE);
      conn.commit();
    } catch (IOException e) {
      throw new AssertionError("Unable to read RDF from file: " + fileName, e);
    }
    return repository;
  }
}
