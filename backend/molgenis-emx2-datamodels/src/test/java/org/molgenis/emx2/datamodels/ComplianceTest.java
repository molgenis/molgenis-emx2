package org.molgenis.emx2.datamodels;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.rdf.RDFService;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/**
 * Not a test itself, but offers to component to quickly build specific tests such as
 * CatalogueComplianceTest and FDPComplianceTest
 */
public abstract class ComplianceTest {

  /**
   * Create a schema according to a profile and return its exported RDF
   *
   * @param schemaName
   * @param profile
   * @return
   */
  public static String createSchemaExportRDF(String schemaName, String profile) {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(schemaName);
    new ImportProfileTask(schema, profile, true).run();
    OutputStream outputStream = new ByteArrayOutputStream();
    var rdf = new RDFService("http://localhost:8080", null);
    rdf.describeAsRDF(outputStream, null, null, null, schema);
    return outputStream.toString();
  }
}
