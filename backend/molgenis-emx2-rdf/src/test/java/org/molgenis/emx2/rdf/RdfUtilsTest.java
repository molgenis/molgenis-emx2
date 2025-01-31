package org.molgenis.emx2.rdf;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RdfUtilsTest {
  static final String TEST_SCHEMA = "TestRdfUtils";
  static final String BASE_URL = "http://molgenis.org/";
  static final String NAMESPACE_IRI = BASE_URL + TEST_SCHEMA + "/api/rdf/";

  static Database database;
  static Schema rdfUtilsTest;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    rdfUtilsTest = database.dropCreateSchema(TEST_SCHEMA);
  }

  @AfterAll
  public static void tearDown() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchema(rdfUtilsTest.getName());
  }

  @Test
  void testSchemaNamespaceRetrieval() {
    Namespace expected = Values.namespace(TEST_SCHEMA, NAMESPACE_IRI);

    Assertions.assertAll(
        () ->
            Assertions.assertEquals(expected, RdfUtils.getSchemaNamespace(BASE_URL, rdfUtilsTest)),
        () ->
            Assertions.assertEquals(
                expected, RdfUtils.getSchemaNamespace(BASE_URL, rdfUtilsTest.getMetadata())));
  }
}
