package org.molgenis.emx2.rdf;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.rdf.RdfUtils.hasIllegalPrefix;
import static org.molgenis.emx2.rdf.RdfUtils.isIllegalPrefix;

import java.util.Map;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class RdfUtilsTest {
  static final String TEST_SCHEMA = "TestRdfUtils";
  static final String BASE_URL = "http://molgenis.org/";
  static final String NAMESPACE_IRI = BASE_URL + TEST_SCHEMA + "/api/rdf/";
  static final ValueFactory valueFactory = SimpleValueFactory.getInstance();

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
  void testPrefixMatcher() {
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

  @Test
  void testSemanticConversion() {
    Map<String, Namespace> namespaces =
        Map.ofEntries(
            entry("rdf", new SimpleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")),
            entry("invalid", new SimpleNamespace("invalid", "thisFieldIsInvalid")));

    assertAll(
        // prefixed name
        () ->
            assertEquals(
                valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                RdfUtils.getSemanticValue(namespaces, "rdf:type")),
        // using IRI even though prefixed name is available
        () ->
            assertEquals(
                valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                RdfUtils.getSemanticValue(
                    namespaces, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")),
        // using IRI when no prefixed name is available
        () ->
            assertEquals(
                valueFactory.createIRI("http://purl.org/dc/terms/title"),
                RdfUtils.getSemanticValue(namespaces, "http://purl.org/dc/terms/title")),
        //  IRI with non-typical schema OR prefix is not configured
        () ->
            assertEquals(
                valueFactory.createIRI("undefinedPrefix:value"),
                RdfUtils.getSemanticValue(namespaces, "undefinedPrefix:value")),

        // incomplete semantics (IRI scheme-only / prefix without value)
        () ->
            assertThrows(
                MolgenisException.class, () -> RdfUtils.getSemanticValue(namespaces, "test")),
        // prefixed name that uses a namespace that results in an invalid IRI
        () ->
            assertThrows(
                IllegalArgumentException.class,
                () -> RdfUtils.getSemanticValue(namespaces, "invalid:test")));
  }
}
