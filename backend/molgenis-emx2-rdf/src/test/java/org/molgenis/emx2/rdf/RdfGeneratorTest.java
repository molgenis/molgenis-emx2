package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.datamodels.DataModels.Profile.PET_STORE;

import graphql.ExecutionResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.graphql.GraphqlExecutor;
import org.molgenis.emx2.rdf.writers.RdfWriter;
import org.molgenis.emx2.rdf.writers.WriterFactory;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RdfGeneratorTest {
  static Database database;
  static Schema petStoreSchema;
  static final String baseURL = "http://localhost:8080";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    String schemaName = "graphql rdf pet store";
    database.dropSchemaIfExists(schemaName);
    PET_STORE.getImportTask(database, schemaName, "", true).run();
    petStoreSchema = database.getSchema(schemaName);
  }

  @Test
  public void testGenerate() {
    GraphqlExecutor executor = new GraphqlExecutor(petStoreSchema);
    //    String query = "{Pet{name,weight}}";
    String query = "{User{username}}";

    ExecutionResult result = executor.executeWithoutSession(query);

    try (OutputStream out = new ByteArrayOutputStream()) {
      try (RdfWriter writer = WriterFactory.MODEL.create(out, RDFFormat.TURTLE)) {
        RdfGenerator generator = new RdfGenerator(writer, baseURL);
        generator.generate(petStoreSchema, result);
      }
      System.out.println(out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    result.getData();
  }
}
