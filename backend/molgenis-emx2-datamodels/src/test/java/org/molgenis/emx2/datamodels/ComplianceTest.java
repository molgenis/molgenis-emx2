package org.molgenis.emx2.datamodels;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.io.ImportProfileTask;
import org.molgenis.emx2.rdf.RDFService;
import org.molgenis.emx2.rdf.RdfSchemaService;
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
    try(RdfSchemaService rdf = new RdfSchemaService("http://localhost:8080", RDFFormat.TURTLE, outputStream)){
      rdf.getGenerator().generate(schema);
    }
    return outputStream.toString();
  }
}
