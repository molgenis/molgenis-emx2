package org.molgenis.emx2.rdf.generators.query;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.*;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class SparqlQueryingExampleTest {

  @Disabled("Used for manual testing purposes")
  @Test
  void testCatalogueQuery() {
    assertDoesNotThrow(
        () -> {
          Database database = TestDatabaseFactory.getTestDatabase();
          String schemaName = "harvesting";
          database.dropSchemaIfExists(schemaName);
          DataModels.Profile.DATA_CATALOGUE
              .getImportTask(database, schemaName, "DCAT harvesting test", true)
              .run();

          SchemaMetadata schema =
              TestDatabaseFactory.getTestDatabase().getSchema("harvesting").getMetadata();
          TableQueryGenerator generator = new TableQueryGenerator();
          String query = generator.generate(schema.getTableMetadata("Collections"));
          System.out.println(query);
        });
  }

  /**
   * Manual test that executes a SPARQL SELECT query from a file against a Turtle RDF dataset loaded
   * from a file, writing the results to a CSV file.
   *
   * <p>To use this test:
   *
   * <ol>
   *   <li>Set {@code absoluteQueryPath} to the absolute path of a file containing a SPARQL SELECT
   *       query.
   *   <li>Set {@code absoluteTtlPath} to the absolute path of a file containing RDF data in Turtle
   *       (.ttl) format.
   *   <li>Results will be written to {@code results.csv} in the working directory.
   * </ol>
   */
  @Disabled("Used for manual testing purposes")
  @Test
  void fileBasedTest() {
    assertDoesNotThrow(
        () -> {
          // Absolute path of a file containing a SPARQL SELECT query.
          String absoluteQueryPath = "";
          String query = new String(new FileInputStream(absoluteQueryPath).readAllBytes());

          // Absolute path of a file containing RDF data in Turtle (.ttl) format.
          String absoluteTtlPath = "";
          SailRepository repository = setupRepositoryFromFile(absoluteTtlPath);

          repository
              .getConnection()
              .prepareTupleQuery(QueryLanguage.SPARQL, query)
              .evaluate(new SPARQLResultsCSVWriter(new FileOutputStream("results.csv")));
        });
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
