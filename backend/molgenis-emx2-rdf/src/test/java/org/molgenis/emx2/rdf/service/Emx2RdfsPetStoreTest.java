package org.molgenis.emx2.rdf.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.IOException;
import java.util.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.rdf.BasicIRI;
import org.molgenis.emx2.rdf.InMemoryRDFHandler;

/** Tests specific for {@link org.molgenis.emx2.rdf.generators.Emx2RdfGenerator} */
class Emx2RdfsPetStoreTest extends PetStoreTest {
  private static final String SCHEMA_NAME_PREFIX = Emx2RdfsPetStoreTest.class.getSimpleName() + "_";

  /**
   * Encoded id for the Pet pooky. The id string is composed by base64 encoding the id columns and
   * their values separately. Column names and values are separated by an ampersand and multiple
   * column / value pairs by a semicolon. Colums are sorted alphabetically for a stable order.
   */
  public static final String POOKY_ROWID = "name=pooky";

  @Test
  void testThatColumnsAreAProperty() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(petStoreTest);

    assertFalse(handler.resources.entrySet().isEmpty());
    for (Map.Entry<Resource, Map<IRI, Set<Value>>> resource : handler.resources.entrySet()) {
      Resource subject = resource.getKey();
      Set<Value> types = resource.getValue().getOrDefault(RDF.TYPE, Set.of());
      if (subject.stringValue().contains("/column/")) {

        assertTrue(
            types.contains(OWL.OBJECTPROPERTY)
                || types.contains(OWL.DATATYPEPROPERTY)
                || types.contains(OWL.ANNOTATEDPROPERTY),
            "Columns must be defined as rdf:type one of owl:objectProperty, owl:dataProperty or owl:annotationProperty");
      }
    }
  }

  @Test
  void testThatTablesAreClasses() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(petStoreTest);

    assertFalse(handler.resources.entrySet().isEmpty());
    for (Map.Entry<Resource, Map<IRI, Set<Value>>> resource : handler.resources.entrySet()) {
      Set<Value> subClasses = resource.getValue().get(RDFS.SUBCLASSOF);
      if (subClasses != null && subClasses.contains(BasicIRI.SIO_DATABASE_TABLE)) {
        Set<Value> types = resource.getValue().getOrDefault(RDF.TYPE, Set.of());
        String subject = resource.getKey().stringValue();
        assertFalse(types.isEmpty(), subject + " should have a rdf:Type.");
        assertTrue(types.contains(OWL.CLASS), subject + " should be a owl:Class.");
      }
    }
  }

  @Test
  void testThatClassesDoNotHaveRangeOrDomain() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(petStoreTest);

    assertFalse(handler.resources.entrySet().isEmpty());
    for (Map.Entry<Resource, Map<IRI, Set<Value>>> resource : handler.resources.entrySet()) {
      String subject = resource.getKey().stringValue();
      Set<IRI> predicates = resource.getValue().keySet();
      Set<Value> types = resource.getValue().get(RDF.TYPE);
      if (types != null && types.contains(OWL.CLASS)) {
        assertFalse(
            predicates.contains(RDFS.DOMAIN),
            subject + " can't have a rdfs:Domain, since it is a class.");
        assertFalse(
            predicates.contains(RDFS.RANGE),
            subject + "can't have a rdfs:Range, since it is a class.");
      }
    }
  }

  @Test
  void testThatColumnsHaveARangeAndDomain() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(petStoreTest);

    assertFalse(handler.resources.entrySet().isEmpty());
    for (Map.Entry<Resource, Map<IRI, Set<Value>>> resource : handler.resources.entrySet()) {
      Resource subject = resource.getKey();
      Set<IRI> predicates = resource.getValue().keySet();
      if (subject.stringValue().contains("/column/")) {
        assertTrue(predicates.contains(RDFS.DOMAIN), subject + " should define a rdfs:Domain");
        assertTrue(predicates.contains(RDFS.RANGE), subject + " should define a rdfs:Range");
      }
    }
  }

  @Test
  void testThatRDFforColumnOnlyContainsMetadata() throws IOException {
    InMemoryRDFHandler handler = parseColumnRdf(petStoreTest, "Pet", "name");
    List<Value> allowedTypes =
        List.of(
            OWL.CLASS,
            OWL.ANNOTATEDPROPERTY,
            OWL.DATATYPEPROPERTY,
            OWL.OBJECTPROPERTY,
            RDFS.CONTAINER,
            BasicIRI.SIO_DATABASE);

    assertFalse(handler.resources.entrySet().isEmpty());
    for (Map.Entry<Resource, Map<IRI, Set<Value>>> resource : handler.resources.entrySet()) {
      String subject = resource.getKey().stringValue();
      Set<Value> types = resource.getValue().getOrDefault(RDF.TYPE, Set.of());
      assertFalse(types.isEmpty(), subject + " should have a rdf:type.");
      boolean isAllowedType = false;
      for (Value type : types) {
        if (allowedTypes.contains(type)) {
          isAllowedType = true;
        }
      }
      assertTrue(
          isAllowedType,
          subject
              + "Should be one of the allowed types ["
              + allowedTypes.stream().map(Objects::toString)
              + "]");
    }
  }

  @Test
  void testCorrectEndpointIRI() throws IOException {
    InMemoryRDFHandler handler = parseRowRdf(petStoreTest, "Pet", POOKY_ROWID);

    Set<Value> endpointIris =
        handler
            .resources
            .get(Values.iri(getApi(petStoreTest) + "Pet/" + POOKY_ROWID))
            .get(Values.iri(DCAT.ENDPOINT_URL.stringValue()));
    assertAll(
        () -> assertEquals(1, endpointIris.size()),
        () ->
            assertEquals(
                Values.iri(getApi(petStoreTest, false)), endpointIris.stream().findFirst().get()));
  }

  @Test
  void testThatInstancesUseReferToDatasetWithTheRightPredicate() throws IOException {
    InMemoryRDFHandler handler = parseRowRdf(petStoreTest, "Pet", POOKY_ROWID);

    assertFalse(handler.resources.keySet().isEmpty());
    for (Resource iri : handler.resources.keySet()) {
      // Select the triples for pooky
      if (iri.stringValue().endsWith(POOKY_ROWID)) {

        Map<IRI, Set<Value>> pooky = handler.resources.get(iri);
        assertTrue(
            pooky.containsKey(BasicIRI.LD_DATASET_PREDICATE),
            "An instance of a Pet should refer back to the Collection using qb:dataSet");
        assertFalse(pooky.containsKey(BasicIRI.LD_DATASET_CLASS), "qb:DataSet is not a predicate");
      }
    }
  }

  @Test
  void testThatColumnPredicatesAreNotSubClasses() throws IOException {
    IRI databaseColumn = Values.iri("http://semanticscience.org/resource/SIO_000757");
    IRI measureProperty = Values.iri("http://purl.org/linked-data/cube#MeasureProperty");
    InMemoryRDFHandler handler = parseColumnRdf(petStoreTest, "Pet", "name");

    assertFalse(handler.resources.keySet().isEmpty());
    for (Resource subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith("/column/name")) {
        Set<Value> subclasses =
            handler.resources.get(subject).getOrDefault(RDFS.SUBCLASSOF, Set.of());
        assertFalse(
            subclasses.contains(databaseColumn), "We don't model as a SIO database column.");
        assertFalse(subclasses.contains(measureProperty), "Measure property should not be used");
        assertTrue(subclasses.isEmpty(), "Predicates are not classes but properties.");
      }
    }
  }

  @Test
  void testThatInstancesAreNotASIODatabaseRow() throws IOException {
    IRI databaseRow = Values.iri("http://semanticscience.org/resource/SIO_001187");
    InMemoryRDFHandler handler = parseRowRdf(petStoreTest, "Pet", POOKY_ROWID);

    assertFalse(handler.resources.keySet().isEmpty());
    for (Resource subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith(POOKY_ROWID)) {
        Set<Value> types = handler.resources.get(subject).get(RDF.TYPE);
        assertFalse(types.contains(databaseRow), "We don't model as a SIO database row.");
      }
    }
  }

  /**
   * Ontology tables are describing classes and their properties are described using RDF Schema.
   *
   * @see <a href="https://github.com/molgenis/molgenis-emx2/issues/2997">Issue #2997</a>
   * @throws IOException
   */
  @Test
  void testThatOntologyTermsDonNotDefineColumnsAsPredicates() throws IOException {
    InMemoryRDFHandler handler = parseTableRdf(petStoreTest, "Tag");

    assertFalse(handler.resources.keySet().isEmpty());
    for (Resource subject : handler.resources.keySet()) {
      assertFalse(
          subject.stringValue().contains("/Tag/column/"),
          "Ontology tables should use standard predicates from RDF Schema.");
    }
  }

  @Test
  void testThatURLColumnsAreObjectProperties() throws IOException {
    final String schemaName = SCHEMA_NAME_PREFIX + "Website";

    try {
      Schema schema = database.dropCreateSchema(schemaName);
      Table table =
          schema.create(table("Websites", column("website", ColumnType.HYPERLINK).setKey(1)));
      table.insert(row("website", "https://www.molgenis.org/"));
      InMemoryRDFHandler handler = parseTableRdf(schema, table.getName());
      boolean isObjectProperty = false;
      boolean linkHasLabel = false;

      assertFalse(handler.resources.keySet().isEmpty());
      for (Resource subject : handler.resources.keySet()) {
        if (subject.stringValue().contains("/column/website")) {
          Set<Value> types = handler.resources.get(subject).get(RDF.TYPE);

          for (Value type : types) {
            if (type.equals(OWL.OBJECTPROPERTY)) {
              isObjectProperty = true;
            }
          }
        }
        if (subject.stringValue().equals("https://www.molgenis.org/")) {
          Set<Value> labels = handler.resources.get(subject).get(RDFS.LABEL);
          for (Value label : labels) {
            if (label.stringValue().equals("https://www.molgenis.org/")) {
              linkHasLabel = true;
            }
          }
        }
      }
      assertTrue(linkHasLabel, "The link should have a label to make it easer to read.");
      assertTrue(isObjectProperty, "The column website should be defined as a Object Property.");
    } finally {
      database.dropSchemaIfExists(schemaName);
    }
  }

  @Test
  void testThatAllInstancesHaveALabel() throws IOException {
    InMemoryRDFHandler handler = parseSchemaRdf(petStoreTest);
    int instancesWithOutALabel = 0;

    assertFalse(handler.resources.keySet().isEmpty());
    for (Resource resource : handler.resources.keySet()) {
      Set<Value> labels = handler.resources.get(resource).get(RDFS.LABEL);
      if (labels.isEmpty()) {
        System.err.println(
            "Each resource should have a label. " + resource.stringValue() + " has none.");
        instancesWithOutALabel += 1;
      }
    }
    assertEquals(0, instancesWithOutALabel, "All instances should have a label.");
  }

  @Test
  void testSubClassesForInheritedTable() throws IOException {
    final String schemaName = SCHEMA_NAME_PREFIX + "InheritTable";

    try {
      Schema schema = database.dropCreateSchema(schemaName);
      Table root = schema.create(table("root", column("id").setPkey()));
      Table child = schema.create(table("child", column("name")).setInheritName("root"));
      InMemoryRDFHandler handler = parseTableRdf(schema, child.getName());
      IRI rootIRI = Values.iri(getApi(schema) + root.getIdentifier());
      IRI childIRI = Values.iri(getApi(schema) + child.getIdentifier());
      IRI cubeDataSetIRI = Values.iri("http://purl.org/linked-data/cube#DataSet");
      Set<Value> subclasses = handler.resources.get(childIRI).get(RDFS.SUBCLASSOF);
      assertEquals(
          2,
          subclasses.size(),
          "Tables that inherit from another table are expected to be only a subclass of that table and DataSet.\n"
              + "Actual result: "
              + subclasses);
      assertTrue(subclasses.contains(rootIRI), "Table is expected to be a subclass of it's parent");
      assertFalse(
          subclasses.contains(OWL.THING),
          "Subclasses are not expected to be a direct subclass of owl:Thing");
      assertTrue(
          subclasses.contains(cubeDataSetIRI),
          "Subclasses are expected to be a subclass of cube@DataSet");
    } finally {
      database.dropSchemaIfExists(schemaName);
    }
  }

  @Test
  void testSubClassRootTables() throws IOException {
    final String schemaName = SCHEMA_NAME_PREFIX + "RootTable";

    try {
      Schema schema = database.dropCreateSchema(schemaName);
      Table root = schema.create(table("root", column("id").setPkey()));
      Table child = schema.create(table("child", column("name")).setInheritName("root"));
      InMemoryRDFHandler handler = parseTableRdf(schema, root.getName());
      IRI rootIRI = Values.iri(getApi(schema) + root.getIdentifier());
      IRI childIRI = Values.iri(getApi(schema) + child.getIdentifier());
      IRI cubeDataSetIRI = Values.iri("http://purl.org/linked-data/cube#DataSet");
      Set<Value> subclasses = handler.resources.get(rootIRI).get(RDFS.SUBCLASSOF);
      assertEquals(
          2,
          subclasses.size(),
          "Tables that do not inherit from another table are expected to be only a subclass owl:Thing and cube#DataSet.\n"
              + "Actual result: "
              + subclasses);
      assertFalse(subclasses.contains(rootIRI), "Table can't be its own parent.");
      assertTrue(
          subclasses.contains(OWL.THING),
          "Subclasses are not expected to be a direct subclass of owl:Thing");
      assertTrue(
          subclasses.contains(cubeDataSetIRI),
          "Subclasses are expected to be a subclass of cube@DataSet");
    } finally {
      database.dropSchemaIfExists(schemaName);
    }
  }
}
