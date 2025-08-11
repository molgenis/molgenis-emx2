package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.rdf.RdfUtils.hasIllegalPrefix;
import static org.molgenis.emx2.rdf.RdfUtils.isIllegalPrefix;

import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RdfUtilsTest {
  static final String TEST_SCHEMA = "TestRdfUtils";
  static final String BASE_URL = "http://molgenis.org";
  // namespace has trailing slash!
  static final String NAMESPACE_IRI = BASE_URL + "/" + TEST_SCHEMA + "/api/rdf/";

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

    assertAll(
        () -> assertEquals(expected, RdfUtils.getSchemaNamespace(BASE_URL, rdfUtilsTest)),
        () ->
            assertEquals(
                expected, RdfUtils.getSchemaNamespace(BASE_URL, rdfUtilsTest.getMetadata())));
  }

  @Test
  void testPrefixValidation() {
    assertAll(
        () -> assertFalse(isIllegalPrefix("dcat")),
        () -> assertTrue(isIllegalPrefix("http")),
        () -> assertTrue(isIllegalPrefix("https")),
        () -> assertFalse(isIllegalPrefix("httprefix")),
        () -> assertTrue(isIllegalPrefix("urn")),
        () -> assertTrue(isIllegalPrefix("urn:uuid")),
        () -> assertTrue(hasIllegalPrefix("http:example.com")),
        () -> assertTrue(hasIllegalPrefix("urn:uuid:6c259a64-d605-4841-b482-d9c0ab81cdf5")));
  }
}
