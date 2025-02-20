package org.molgenis.emx2.rdf;

import static java.util.Map.entry;

import java.util.Map;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
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

    Assertions.assertAll(
        () ->
            Assertions.assertEquals(expected, RdfUtils.getSchemaNamespace(BASE_URL, rdfUtilsTest)),
        () ->
            Assertions.assertEquals(
                expected, RdfUtils.getSchemaNamespace(BASE_URL, rdfUtilsTest.getMetadata())));
  }

  @Test
  void testSemanticConversion() {
    Map<String, Namespace> namespaces =
        Map.ofEntries(
            entry("rdf", new SimpleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")),
            entry("invalid", new SimpleNamespace("invalid", "thisFieldIsInvalid")));

    Assertions.assertAll(
        () -> // valid prefixed name
        Assertions.assertEquals(
                valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                RdfUtils.getSemanticValue(namespaces, "rdf:type")),
        () -> // valid IRI
        Assertions.assertEquals(
                valueFactory.createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                RdfUtils.getSemanticValue(
                    namespaces, "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")),
        () -> // invalid IRI
        Assertions.assertThrows(
                MolgenisException.class,
                () ->
                    RdfUtils.getSemanticValue(
                        namespaces, "http://www.w3.org/2000/01/rdf-schema#label")),
        () -> // non-existing prefix label
        Assertions.assertThrows(
                MolgenisException.class,
                () -> RdfUtils.getSemanticValue(namespaces, "doesnotexist:test")),
        () -> // existing but invalid namespace
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> RdfUtils.getSemanticValue(namespaces, "invalid:test")));
  }
}
