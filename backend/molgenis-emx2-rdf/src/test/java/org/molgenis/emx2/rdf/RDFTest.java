package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.datamodels.DataModels.Regular.PET_STORE;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.SKOS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RDFTest {

  /**
   * Encoded id for the Pet pooky. The id string is composed by base64 encoding the id columns and
   * their values separately. Column names and values are separated by an ampersand and multiple
   * column / value pairs by a semicolon. Colums are sorted alphabetically for a stable order.
   */
  public static final String POOKY_ROWID = "name=pooky";

  /** Advanced setting field for adding custom RDF to the API. */
  private static final String SETTING_CUSTOM_RDF = "custom_rdf";

  static final ClassLoader classLoader = ColumnTypeRdfMapperTest.class.getClassLoader();

  static Database database;
  static List<Schema> petStoreSchemas;
  static final String RDF_API_LOCATION = "/api/rdf";
  static Schema petStore_nr1;
  static Schema petStore_nr2;
  static Schema compositeKeyTest;
  static Schema ontologyTest;
  static Schema tableInherTest;
  static Schema tableInherExtTest;
  static Schema fileTest;
  static Schema refBackTest;

  final Set<Namespace> DEFAULT_NAMESPACES =
      new HashSet<>() {
        {
          add(new SimpleNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"));
          add(new SimpleNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#"));
          add(new SimpleNamespace("xsd", "http://www.w3.org/2001/XMLSchema#"));
          add(new SimpleNamespace("owl", "http://www.w3.org/2002/07/owl#"));
          add(new SimpleNamespace("sio", "http://semanticscience.org/resource/"));
          add(new SimpleNamespace("qb", "http://purl.org/linked-data/cube#"));
          add(new SimpleNamespace("skos", "http://www.w3.org/2004/02/skos/core#"));
          add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
          add(new SimpleNamespace("dcat", "http://www.w3.org/ns/dcat#"));
          add(new SimpleNamespace("foaf", "http://xmlns.com/foaf/0.1/"));
          add(new SimpleNamespace("vcard", "http://www.w3.org/2006/vcard/ns#"));
          add(new SimpleNamespace("org", "http://www.w3.org/ns/org#"));
          add(new SimpleNamespace("fdp-o", "https://w3id.org/fdp/fdp-o#"));
        }
      };

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    petStore_nr1 = database.dropCreateSchema("petStoreNr1");
    petStore_nr2 = database.dropCreateSchema("petStoreNr2");
    PET_STORE.getImportTask(petStore_nr1, true).run();
    PET_STORE.getImportTask(petStore_nr2, true).run();
    petStoreSchemas = List.of(petStore_nr1, petStore_nr2);

    // Test schema for composite keys
    compositeKeyTest = database.dropCreateSchema(RDFTest.class.getSimpleName() + "_compositeKey");
    compositeKeyTest.create(
        table("Patients", column("firstName").setPkey(), column("lastName").setPkey()),
        table(
            "Samples",
            column("patient").setType(ColumnType.REF).setRefTable("Patients").setPkey(),
            column("id").setPkey(),
            column("someNonKeyRef").setType(ColumnType.REF_ARRAY).setRefTable("Samples")));
    compositeKeyTest.getTable("Patients").insert(row("firstName", "Donald", "lastName", "Duck"));
    compositeKeyTest
        .getTable("Samples")
        .insert(
            row("patient.firstName", "Donald", "patient.lastName", "Duck", "id", "sample1"),
            row(
                "patient.firstName",
                "Donald",
                "patient.lastName",
                "Duck",
                "id",
                "sample2",
                "someNonKeyRef.patient.firstName",
                "Donald",
                "someNonKeyRef.patient.lastName",
                "Duck",
                "someNonKeyRef.id",
                "sample1"));

    // Test schema for ontologies
    ontologyTest = database.dropCreateSchema("OntologyTest");
    ontologyTest.create(table("Diseases").setTableType(TableType.ONTOLOGIES));
    ontologyTest
        .getTable("Diseases")
        .insert(
            row(
                "order",
                0,
                "name",
                "U07",
                "label",
                "Emergency Use of U07",
                "codesystem",
                "ICD-10",
                "code",
                "U07",
                "ontologyTermURI",
                "https://icd.who.int/browse10/2019/en#/U07",
                "definition",
                "Codes used by WHO for the provisional assignment of new diseases of uncertain etiology."),
            row(
                "order",
                1,
                "name",
                "U07.1",
                "label",
                "COVID-19",
                "parent",
                "U07",
                "codesystem",
                "ICD-10",
                "code",
                "U07.1",
                "ontologyTermURI",
                "https://icd.who.int/browse10/2019/en#/U07.1",
                "definition",
                "COVID-19 NOS"),
            row(
                "order",
                2,
                "name",
                "C00-C75 Malignant neoplasms, stated or presumed to be primary, of specified sites, except of lymphoid, haematopoietic and related tissue",
                "code",
                "C00-C75"),
            row(
                "order",
                3,
                "name",
                "C00-C14 Malignant neoplasms of lip, oral cavity and pharynx",
                "parent",
                "C00-C75 Malignant neoplasms, stated or presumed to be primary, of specified sites, except of lymphoid, haematopoietic and related tissue",
                "code",
                "C00-C14"));

    // Test table inheritance
    // Use example from the catalogue schema since this has all the different issues.
    tableInherTest = database.dropCreateSchema("tableInheritanceTest");
    tableInherTest.create(
        table(
            "Root",
            column("id", ColumnType.STRING).setKey(1),
            column("rootColumn", ColumnType.STRING)));
    tableInherTest.create(table("Child", column("childColumn")).setInheritName("Root"));
    // Same column name but not in shared parent, so test how this is handled.
    tableInherTest.create(
        table("GrandchildTypeA", column("grandchildColumn")).setInheritName("Child"));
    tableInherTest.create(
        table("GrandchildTypeB", column("grandchildColumn")).setInheritName("Child"));
    tableInherTest.getTable("Root").insert(row("id", "1", "rootColumn", "id1 data"));
    tableInherTest.getTable("Child").insert(row("id", "2", "childColumn", "id2 data"));
    tableInherTest
        .getTable("GrandchildTypeA")
        .insert(row("id", "3", "grandchildColumn", "id3 data"));
    tableInherTest
        .getTable("GrandchildTypeB")
        .insert(
            row(
                "id",
                "4",
                "rootColumn",
                "id4 data for rootColumn",
                "childColumn",
                "id4 data for childColumn",
                "grandchildColumn",
                "id4 data"));

    // Test for table that extends table from different schema
    tableInherExtTest = database.createSchema("tableInheritanceExternalSchemaTest");
    tableInherExtTest.create(
        table("ExternalChild", column("externalChildColumn"))
            .setImportSchema(tableInherTest.getName())
            .setInheritName("Root"));
    tableInherExtTest.create(
        table("ExternalGrandchild", column("externalGrandchildColumn"))
            .setInheritName("ExternalChild"));
    tableInherExtTest.create(
        table(
            "ExternalUnrelated",
            column("id", ColumnType.STRING).setKey(1),
            column("externalUnrelatedColumn")));
    tableInherExtTest
        .getTable("ExternalChild")
        .insert(row("id", "5", "externalChildColumn", "id5 data"));
    tableInherExtTest
        .getTable("ExternalGrandchild")
        .insert(row("id", "6", "externalGrandchildColumn", "id6 data"));
    tableInherExtTest
        .getTable("ExternalUnrelated")
        .insert(row("id", "a", "externalUnrelatedColumn", "unrelated data"));

    // Test FILE
    fileTest = database.dropCreateSchema("fileTest");
    fileTest.create(
        table(
            "myFiles",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("file").setType(ColumnType.FILE)));

    fileTest
        .getTable("myFiles")
        .insert(
            row(
                "id",
                "1",
                "file",
                new File(classLoader.getResource("testfiles/molgenis.png").getFile())));

    // Refback test (petstore refback uses auto id)
    refBackTest = database.dropCreateSchema("refBackTest");
    refBackTest.create(
        table(
            "tableRef",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("link").setType(ColumnType.REF).setRefTable("tableRefBack")),
        table("tableRefBack", column("id").setType(ColumnType.STRING).setPkey()));
    refBackTest
        .getTable("tableRefBack")
        .getMetadata()
        .add(
            column("backlink")
                .setType(ColumnType.REFBACK)
                .setRefTable("tableRef")
                .setRefBack("link"));

    refBackTest.getTable("tableRefBack").insert(row("id", "a"));
    refBackTest.getTable("tableRef").insert(row("id", "1", "link", "a"));
  }

  @AfterAll
  public static void tearDown() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchema(petStore_nr1.getName());
    database.dropSchema(petStore_nr2.getName());
    database.dropSchema(compositeKeyTest.getName());
    database.dropSchema(ontologyTest.getName());
    database.dropSchema(tableInherExtTest.getName());
    database.dropSchema(tableInherTest.getName());
    database.dropSchema(fileTest.getName());
    database.dropSchema(refBackTest.getName());
  }

  @Test
  void testThatColumnsAreAProperty() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1), handler);

    for (var resource : handler.resources.entrySet()) {
      var subject = resource.getKey();
      var types = resource.getValue().getOrDefault(RDF.TYPE, Set.of());
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
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1), handler);

    for (var resource : handler.resources.entrySet()) {
      var subClasses = resource.getValue().get(RDFS.SUBCLASSOF);
      if (subClasses != null && subClasses.contains(RDFService.IRI_DATABASE_TABLE)) {
        var types = resource.getValue().getOrDefault(RDF.TYPE, Set.of());
        var subject = resource.getKey().stringValue();
        assertFalse(types.isEmpty(), subject + " should have a rdf:Type.");
        assertTrue(types.contains(OWL.CLASS), subject + " should be a owl:Class.");
      }
    }
  }

  @Test
  void testThatClassesDoNotHaveRangeOrDomain() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1), handler);

    for (var resource : handler.resources.entrySet()) {
      var subject = resource.getKey().stringValue();
      var predicates = resource.getValue().keySet();
      var types = resource.getValue().get(RDF.TYPE);
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
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1), handler);
    for (var resource : handler.resources.entrySet()) {
      var subject = resource.getKey();
      var predicates = resource.getValue().keySet();
      if (subject.stringValue().contains("/column/")) {
        assertTrue(predicates.contains(RDFS.DOMAIN), subject + " should define a rdfs:Domain");
        assertTrue(predicates.contains(RDFS.RANGE), subject + " should define a rdfs:Range");
      }
    }
  }

  @Test
  void testThatRDFOnlyIncludesRequestedSchema() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1), handler);
    for (var resource : handler.resources.keySet()) {
      assertFalse(
          resource.toString().contains("petStoreNr2"),
          "No resources within the petStoreNr2 schema should be included.");
    }
  }

  @Test
  void testThatRDFforColumnOnlyContainsMetadata() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1, "Pet", "name"), handler);
    List<Value> allowedTypes =
        List.of(
            OWL.CLASS,
            OWL.ANNOTATEDPROPERTY,
            OWL.DATATYPEPROPERTY,
            OWL.OBJECTPROPERTY,
            RDFS.CONTAINER,
            RDFService.IRI_DATABASE);

    for (var resource : handler.resources.entrySet()) {
      var subject = resource.getKey().stringValue();
      var types = resource.getValue().getOrDefault(RDF.TYPE, Set.of());
      assertFalse(types.isEmpty(), subject + " should have a rdf:type.");
      boolean isAllowedType = false;
      for (var type : types) {
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
  void testThatCompositeKeysReferToDatabaseFields() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(compositeKeyTest), handler);
    var subjectWithCompositeKey =
        "http://localhost:8080/"
            + compositeKeyTest.getName()
            + "/api/rdf/Samples?id=sample1&patient.firstName=Donald&patient.lastName=Duck";
    var iris = handler.resources.keySet().stream().map(Objects::toString).toList();
    assertTrue(
        iris.contains(subjectWithCompositeKey),
        "A Sample resource should have a key based on patient.firstName, patient.lastName and id");
  }

  @Test
  void testThatRowCanBeFetchedByCompositeKey() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    // Encoded version of patient.firstName=Donald & patient.lastName=Duck & id=sample1
    var rowId = "id=sample2&patient.firstName=Donald&patient.lastName=Duck";
    getAndParseRDF(Selection.ofRow(compositeKeyTest, "Samples", rowId), handler);
    var subjectWithCompositeKey =
        "http://localhost:8080/" + compositeKeyTest.getName() + "/api/rdf/Samples?" + rowId;
    var iris = handler.resources.keySet().stream().map(Objects::toString).toList();
    assertTrue(
        iris.contains(subjectWithCompositeKey),
        "A Sample resource should have a key based on patient.firstName, patient.lastName and id");
  }

  @Test
  void testThatInstancesUseReferToDatasetWithTheRightPredicate() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(petStore_nr1, "Pet", POOKY_ROWID), handler);
    for (var iri : handler.resources.keySet()) {
      // Select the triples for pooky
      if (iri.stringValue().endsWith(POOKY_ROWID)) {

        var pooky = handler.resources.get(iri);
        assertTrue(
            pooky.containsKey(RDFService.IRI_DATASET_PREDICATE),
            "An instance of a Pet should refer back to the Collection using qb:dataSet");
        assertFalse(
            pooky.containsKey(RDFService.IRI_DATASET_CLASS), "qb:DataSet is not a predicate");
      }
    }
  }

  @Test
  void testThatColumnPredicatesAreNotSubClasses() throws IOException {
    var database_column = Values.iri("http://semanticscience.org/resource/SIO_000757");
    var measure_property = Values.iri("http://purl.org/linked-data/cube#MeasureProperty");
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1, "Pet", "name"), handler);
    for (var subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith("/column/name")) {
        var subclasses = handler.resources.get(subject).getOrDefault(RDFS.SUBCLASSOF, Set.of());
        assertFalse(
            subclasses.contains(database_column), "We don't model as a SIO database column.");
        assertFalse(subclasses.contains(measure_property), "Measure property should not be used");
        assertTrue(subclasses.isEmpty(), "Predicates are not classes but properties.");
      }
    }
  }

  @Test
  void testThatInstancesAreNotASIODatabaseRow() throws IOException {
    var database_row = Values.iri("http://semanticscience.org/resource/SIO_001187");
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(petStore_nr1, "Pet", POOKY_ROWID), handler);
    for (var subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith(POOKY_ROWID)) {
        var types = handler.resources.get(subject).get(RDF.TYPE);
        assertFalse(types.contains(database_row), "We don't model as a SIO database row.");
      }
    }
  }

  /**
   * Ontology tables are describing classes.
   *
   * @see <a href="https://github.com/molgenis/molgenis-emx2/issues/2984">Issue #2997</a>
   * @throws IOException
   */
  @Test
  void testThatOntologyTermsAreClasses() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(ontologyTest, "Diseases"), handler);
    for (var subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith("/Diseases/U07.1")) {
        var types = handler.resources.get(subject).get(RDF.TYPE);
        assertTrue(types.contains(OWL.CLASS), "Ontology tables define classes");
      }
    }
  }

  @Test
  void testThatOntologyTermsUseRDFSchema() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(ontologyTest, "Diseases"), handler);
    for (var subject : handler.resources.keySet()) {
      if (subject.stringValue().endsWith("/Diseases/U07.1")) {
        var data = handler.resources.get(subject);
        assertTrue(data.containsKey(RDFS.LABEL), "The class should have a label");
        assertTrue(
            data.containsKey(RDFS.SUBCLASSOF),
            "Children should be defined as a subClass of a parent Class");
        assertTrue(
            data.containsKey(OWL.SAMEAS),
            "URLs to the canonical version should be defined a owl:sameAs");
        assertTrue(
            data.containsKey(RDFS.ISDEFINEDBY), "Definition should be given as rdsf:isDefinedBy");
        assertTrue(data.containsKey(SKOS.NOTATION), "Code should be defined as a skos:Notation");
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
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1, "Tag"), handler);
    for (var subject : handler.resources.keySet()) {
      assertFalse(
          subject.stringValue().contains("/Tag/column/"),
          "Ontology tables should use standard predicates from RDF Schema.");
    }
  }

  @Test
  void testThatURLsAreNotSplitForOntologyParentItem() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(ontologyTest, "Diseases"), handler);
    var subject =
        Values.iri(
            "http://localhost:8080/OntologyTest/api/rdf/Diseases?name=C00-C14+Malignant+neoplasms+of+lip%2C+oral+cavity+and+pharynx");

    var parents = handler.resources.get(subject).get(RDFS.SUBCLASSOF);
    assertEquals(
        2, parents.size(), "This disease should only be a subclass of Diseases and C00-C75");
  }

  @Test
  void testTableInheritanceAlwaysSamePredicate() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, tableInherExtTest), handler);
    // All should use the same predicate for rootColumn:
    // Root (is root of all inheritance)
    // Child extends Root
    // GrandChildTypeA extends Child
    // GrandChildTypeB extends Child
    // ExternalChild extends Root
    // ExternalGrandchild extends ExternalChild
    assertAll(
        () ->
            assertTrue(
                handler.resources.containsKey(
                    Values.iri(
                        "http://localhost:8080/tableInheritanceTest/api/rdf/Root/column/rootColumn")),
                "There should be a predicate for the rootColumn in the Root table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(
                        "http://localhost:8080/tableInheritanceTest/api/rdf/Child/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the Child table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(
                        "http://localhost:8080/tableInheritanceTest/api/rdf/GrandchildTypeA/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the GrandchildTypeA table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(
                        "http://localhost:8080/tableInheritanceTest/api/rdf/GrandchildTypeB/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the GrandchildTypeB table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(
                        "http://localhost:8080/tableInheritanceTest/api/rdf/ExternalChild/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the ExternalChild table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(
                        "http://localhost:8080/tableInheritanceTest/api/rdf/ExternalGrandchild/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the ExternalGrandchild table"));
  }

  @Test
  void testTableInheritanceRetrieveData() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, true),
            Map.entry(ValidationTriple.ID2, true),
            Map.entry(ValidationTriple.ID3, true),
            Map.entry(ValidationTriple.ID4, true),
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true),
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true),
            Map.entry(ValidationTriple.ID5, false), // different schema
            Map.entry(ValidationTriple.ID6, false), // different schema
            Map.entry(ValidationTriple.UNRELATED, false) // different schema
            ));
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableRoot() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "Root"), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, true),
            Map.entry(ValidationTriple.ID2, true),
            Map.entry(ValidationTriple.ID3, true),
            Map.entry(ValidationTriple.ID4, true),
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true),
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true),
            Map.entry(ValidationTriple.ID5, false), // different schema
            Map.entry(ValidationTriple.ID6, false), // different schema
            Map.entry(ValidationTriple.UNRELATED, false) // different schema
            ));
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableChild() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "Child"), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // parent of selected table
            Map.entry(ValidationTriple.ID2, true),
            Map.entry(ValidationTriple.ID3, true), // child
            Map.entry(ValidationTriple.ID4, true), // child
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true), // child
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true), // child
            Map.entry(ValidationTriple.ID5, false), // different schema
            Map.entry(ValidationTriple.ID6, false), // different schema
            Map.entry(ValidationTriple.UNRELATED, false) // different schema
            ));
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableGrandchildTypeA() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "GrandchildTypeA"), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // grandparent of selected table
            Map.entry(ValidationTriple.ID2, false), // parent of selected table
            Map.entry(ValidationTriple.ID3, true),
            Map.entry(ValidationTriple.ID4, false), // sibling of selected table
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, false), // sibling of selected table
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, false), // sibling of selected table
            Map.entry(ValidationTriple.ID5, false), // different schema
            Map.entry(ValidationTriple.ID6, false), // different schema
            Map.entry(ValidationTriple.UNRELATED, false) // different schema
            ));
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableGrandchildTypeB() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "GrandchildTypeB"), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // grandparent of selected table
            Map.entry(ValidationTriple.ID2, false), // parent of selected table
            Map.entry(ValidationTriple.ID3, false), // sibling of selected table
            Map.entry(ValidationTriple.ID4, true),
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true),
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true),
            Map.entry(ValidationTriple.ID5, false), // different schema
            Map.entry(ValidationTriple.ID6, false), // different schema
            Map.entry(ValidationTriple.UNRELATED, false) // different schema
            ));
  }

  @Test
  void testTableInheritanceRetrieveDataWithRowId() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(tableInherTest, "Root", "id=4"), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // not selected
            Map.entry(ValidationTriple.ID2, false), // not selected
            Map.entry(ValidationTriple.ID3, false), // not selected
            Map.entry(ValidationTriple.ID4, true),
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true),
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true),
            Map.entry(ValidationTriple.ID5, false), // not selected
            Map.entry(ValidationTriple.ID6, false), // not selected
            Map.entry(ValidationTriple.UNRELATED, false) // not selected
            ));
  }

  @Test
  void testTableInheritanceExternalSchemaRetrieveData() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherExtTest), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // different schema
            Map.entry(ValidationTriple.ID2, false), // different schema
            Map.entry(ValidationTriple.ID3, false), // different schema
            Map.entry(ValidationTriple.ID4, false), // different schema
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, false), // different schema
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, false), // different schema
            Map.entry(ValidationTriple.ID5, true),
            Map.entry(ValidationTriple.ID6, true),
            Map.entry(ValidationTriple.UNRELATED, true)));
  }

  @Test
  void testTableInheritanceExternalSchemaDataWithTableExternalChild() throws IOException {
    // Note that even though the subject has an ID IRI based on table Root, this table is not part
    // of the selected scheme so this table cannot be selected:
    // `tableInherExtTest.getTable("Root")` == `null`
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherExtTest, "ExternalChild"), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // different schema
            Map.entry(ValidationTriple.ID2, false), // different schema
            Map.entry(ValidationTriple.ID3, false), // different schema
            Map.entry(ValidationTriple.ID4, false), // different schema
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, false), // different schema
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, false), // different schema
            Map.entry(ValidationTriple.ID5, true),
            Map.entry(ValidationTriple.ID6, true),
            Map.entry(ValidationTriple.UNRELATED, false))); // not part of inheritance
  }

  @Test
  void testTableInheritanceExternalSchemaDataWithRowId() throws IOException {
    // Note that even though the subject has an ID IRI based on table Root, this table is not part
    // of the selected scheme so this table cannot be selected:
    // `tableInherExtTest.getTable("Root")` == `null`
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(tableInherExtTest, "ExternalChild", "id=5"), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // not selected
            Map.entry(ValidationTriple.ID2, false), // not selected
            Map.entry(ValidationTriple.ID3, false), // not selected
            Map.entry(ValidationTriple.ID4, false), // not selected
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, false), // not selected
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, false), // not selected
            Map.entry(ValidationTriple.ID5, true),
            Map.entry(ValidationTriple.ID6, false), // not selected
            Map.entry(ValidationTriple.UNRELATED, false) // not selected
            ));
  }

  @Test
  void testTableInheritanceRetrieveDataMultiSchema() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, tableInherExtTest), handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, true),
            Map.entry(ValidationTriple.ID2, true),
            Map.entry(ValidationTriple.ID3, true),
            Map.entry(ValidationTriple.ID4, true),
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true),
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true),
            Map.entry(ValidationTriple.ID5, true),
            Map.entry(ValidationTriple.ID6, true),
            Map.entry(ValidationTriple.UNRELATED, true)));
  }

  @Test
  void testTableInheritanceRetrieveDataMultiSchemaWithTableRoot() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(
        Selection.of(
            new Schema[] {tableInherTest, tableInherExtTest}, tableInherTest.getTable("Root")),
        handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, true),
            Map.entry(ValidationTriple.ID2, true),
            Map.entry(ValidationTriple.ID3, true),
            Map.entry(ValidationTriple.ID4, true),
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true),
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true),
            Map.entry(ValidationTriple.ID5, true),
            Map.entry(ValidationTriple.ID6, true),
            Map.entry(ValidationTriple.UNRELATED, false))); // not part of inheritance
  }

  @Test
  void testTableInheritanceRetrieveDataMultiSchemaWithRowId() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(
        Selection.ofRow(
            new Schema[] {tableInherTest, tableInherExtTest},
            tableInherTest.getTable("Root"),
            "id=4"),
        handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // not selected
            Map.entry(ValidationTriple.ID2, false), // not selected
            Map.entry(ValidationTriple.ID3, false), // not selected
            Map.entry(ValidationTriple.ID4, true),
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, true),
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, true),
            Map.entry(ValidationTriple.ID5, false), // not selected
            Map.entry(ValidationTriple.ID6, false), // not selected
            Map.entry(ValidationTriple.UNRELATED, false))); // not selected
  }

  @Test
  void testTableInheritanceRetrieveDataMultiSchemaWithExternalRowId() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(
        Selection.ofRow(
            new Schema[] {tableInherTest, tableInherExtTest},
            tableInherTest.getTable("Root"),
            "id=5"),
        handler);
    assertPresence(
        handler,
        Map.ofEntries(
            Map.entry(ValidationTriple.ID1, false), // not selected
            Map.entry(ValidationTriple.ID2, false), // not selected
            Map.entry(ValidationTriple.ID3, false), // not selected
            Map.entry(ValidationTriple.ID4, false), // not selected
            Map.entry(ValidationTriple.ID4_PARENT_FIELD, false), // not selected
            Map.entry(ValidationTriple.ID4_GRANDPARENT_FIELD, false), // not selected
            Map.entry(ValidationTriple.ID5, true),
            Map.entry(ValidationTriple.ID6, false), // not selected
            Map.entry(ValidationTriple.UNRELATED, false))); // not selected
  }

  @Test
  void testThatURLColumnsAreObjectProperties() throws IOException {
    var schema = database.dropCreateSchema("Website");
    var table = schema.create(table("Websites", column("website", ColumnType.HYPERLINK).setKey(1)));
    table.insert(row("website", "https://www.molgenis.org/"));
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(schema, table.getName()), handler);
    boolean isObjectProperty = false;
    boolean linkHasLabel = false;
    for (var subject : handler.resources.keySet()) {
      if (subject.stringValue().contains("/column/website")) {
        var types = handler.resources.get(subject).get(RDF.TYPE);

        for (var type : types) {
          if (type.equals(OWL.OBJECTPROPERTY)) {
            isObjectProperty = true;
          }
        }
      }
      if (subject.stringValue().equals("https://www.molgenis.org/")) {
        var labels = handler.resources.get(subject).get(RDFS.LABEL);
        for (var label : labels) {
          if (label.stringValue().equals("https://www.molgenis.org/")) {
            linkHasLabel = true;
          }
        }
      }
    }
    assertTrue(linkHasLabel, "The link should have a label to make it easer to read.");
    assertTrue(isObjectProperty, "The column website should be defined as a Object Property.");
    database.dropSchema("Website");
  }

  @Test
  void testThatAllInstancesHaveALabel() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1), handler);
    int instancesWithOutALabel = 0;
    for (var resource : handler.resources.keySet()) {
      var labels = handler.resources.get(resource).get(RDFS.LABEL);
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
    Schema schema = database.dropCreateSchema(RDFTest.class.getSimpleName() + "_InheritTable");
    Table root = schema.create(table("root", column("id").setPkey()));
    Table child = schema.create(table("child", column("name")).setInheritName("root"));
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(schema, child.getName()), handler);
    var rootIRI =
        Values.iri(
            "http://localhost:8080/" + schema.getName() + "/api/rdf/" + root.getIdentifier());
    var childIRI =
        Values.iri(
            "http://localhost:8080/" + schema.getName() + "/api/rdf/" + child.getIdentifier());
    var cubeDataSetIRI = Values.iri("http://purl.org/linked-data/cube#DataSet");
    var subclasses = handler.resources.get(childIRI).get(RDFS.SUBCLASSOF);
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
  }

  @Test
  void testSubClassRootTables() throws IOException {
    Schema schema = database.dropCreateSchema(RDFTest.class.getSimpleName() + "_RootTable");
    Table root = schema.create(table("root", column("id").setPkey()));
    Table child = schema.create(table("child", column("name")).setInheritName("root"));
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(schema, root.getName()), handler);
    var rootIRI =
        Values.iri(
            "http://localhost:8080/" + schema.getName() + "/api/rdf/" + root.getIdentifier());
    var childIRI =
        Values.iri(
            "http://localhost:8080/" + schema.getName() + "/api/rdf/" + child.getIdentifier());
    var cubeDataSetIRI = Values.iri("http://purl.org/linked-data/cube#DataSet");
    var subclasses = handler.resources.get(rootIRI).get(RDFS.SUBCLASSOF);
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
  }

  @Test
  void testCustomRdfSetting() throws IOException {
    final Set<Namespace> defaultNamespaces =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "CustomRdfEdit", "http://localhost:8080/CustomRdfEdit/api/rdf/"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final Set<Namespace> customNamespaces =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "CustomRdfEdit", "http://localhost:8080/CustomRdfEdit/api/rdf/"));
            add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
          }
        };

    final String customRdf =
        """
@prefix dcterms: <http://purl.org/dc/terms/> .
<https://molgenis.org/> dcterms:title "Molgenis" .
""";

    var customRdfEdit = database.dropCreateSchema("CustomRdfEdit");
    // Test default behaviour.
    assertFalse(customRdfEdit.hasSetting(SETTING_CUSTOM_RDF));
    var handlerBefore = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(customRdfEdit), handlerBefore);
    assertEquals(defaultNamespaces, handlerBefore.namespaces);
    assertFalse(handlerBefore.resources.containsKey(Values.iri("https://molgenis.org/")));

    // Change setting
    customRdfEdit.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);

    // Test behaviour after changing setting.
    var handlerAfter = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(customRdfEdit), handlerAfter);
    assertEquals(customNamespaces, handlerAfter.namespaces);
    assertTrue(
        handlerAfter
            .resources
            .get(Values.iri("https://molgenis.org/"))
            .get(Values.iri("http://purl.org/dc/terms/title"))
            .contains(Values.literal("Molgenis")));
  }

  /**
   * While setting the custom_RDF does not validate, trying to use the RDF API will result in an
   * error if invalid RDF is given. In this case a dot is missing to indicate the end of the triple.
   *
   * @throws IOException
   */
  @Test
  void testInvalidCustomRdfSetting() throws IOException {
    final String customRdf =
        """
<https://molgenis.org/> <http://purl.org/dc/terms/title> "Molgenis"
""";

    var customRdfEdit = database.dropCreateSchema("CustomInvalidRdf");
    customRdfEdit.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);
    var handler = new InMemoryRDFHandler() {};
    assertThrows(
        MolgenisException.class, () -> getAndParseRDF(Selection.of(customRdfEdit), handler));
  }

  @Test
  void testDuplicateNamespaces() throws IOException {
    final Set<Namespace> expectedNamespace =
        new HashSet<>() {
          {
            add(new SimpleNamespace("RdfEqual1", "http://localhost:8080/RdfEqual1/api/rdf/"));
            add(new SimpleNamespace("RdfEqual2", "http://localhost:8080/RdfEqual2/api/rdf/"));
            add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
          }
        };

    final String customRdf1 =
        """
@prefix dcterms: <http://purl.org/dc/terms/> .
""";

    final String customRdf2 =
        """
@prefix dcterms: <http://purl.org/dc/terms/> .
""";

    var handler = new InMemoryRDFHandler() {};
    validateNamespaces(handler, "RdfEqual", expectedNamespace, customRdf1, customRdf2);
  }

  /**
   * If 2 namespaces share the same IRI, the first one is kept and used for everything.
   *
   * @throws IOException
   */
  @Test
  void testNamespaceDifferentPrefixSameUrl() throws IOException {
    final Set<Namespace> expectedNamespace =
        new HashSet<>() {
          {
            add(new SimpleNamespace("RdfPrefix1", "http://localhost:8080/RdfPrefix1/api/rdf/"));
            add(new SimpleNamespace("RdfPrefix2", "http://localhost:8080/RdfPrefix2/api/rdf/"));
            add(new SimpleNamespace("dcterms1", "http://purl.org/dc/terms/"));
          }
        };

    final String customRdf1 =
        """
@prefix dcterms1: <http://purl.org/dc/terms/> .
<https://molgenis.org/> dcterms:title "Molgenis" .
""";

    final String customRdf2 =
        """
@prefix dcterms2: <http://purl.org/dc/terms/> .
<https://github.com/molgenis/> dcterms2:title "Molgenis GitHub" .
""";

    var handler = new InMemoryRDFHandler() {};
    validateNamespaces(handler, "RdfPrefix", expectedNamespace, customRdf1, customRdf2);
    assertTrue(
        handler
            .resources
            .get(Values.iri("https://molgenis.org/"))
            .get(Values.iri("http://purl.org/dc/terms/title"))
            .contains(Values.literal("Molgenis")));
    assertTrue(
        handler
            .resources
            .get(Values.iri("https://github.com/molgenis/"))
            .get(Values.iri("http://purl.org/dc/terms/title"))
            .contains(Values.literal("Molgenis GitHub")));
  }

  /**
   * If multiple namespace share the same prefix but refer to a different IRI, they get overwritten
   * by the last to be added. However, the other IRIs are not broken but simply not shortened.
   *
   * @throws IOException
   */
  @Test
  void testNamespaceDifferentUrlSamePrefix() throws IOException {
    final Set<Namespace> expectedNamespace =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "RdfPrefixUrl1", "http://localhost:8080/RdfPrefixUrl1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "RdfPrefixUrl2", "http://localhost:8080/RdfPrefixUrl2/api/rdf/"));
            add(new SimpleNamespace("name", "http://www.w3.org/2000/01/rdf-schema#"));
          }
        };

    final String customRdf1 =
        """
    @prefix name: <http://purl.org/dc/terms/> .
    <https://molgenis.org/> name:title "Molgenis" .
    """;

    final String customRdf2 =
        """
    @prefix name: <http://www.w3.org/2000/01/rdf-schema#> .
    <https://molgenis.org/> name:label "Molgenis" .
    """;

    var handler = new InMemoryRDFHandler() {};
    validateNamespaces(handler, "RdfPrefixUrl", expectedNamespace, customRdf1, customRdf2);

    assertTrue(
        handler
            .resources
            .get(Values.iri("https://molgenis.org/"))
            .get(Values.iri("http://purl.org/dc/terms/title"))
            .contains(Values.literal("Molgenis")));
    assertTrue(
        handler
            .resources
            .get(Values.iri("https://molgenis.org/"))
            .get(Values.iri("http://www.w3.org/2000/01/rdf-schema#label"))
            .contains(Values.literal("Molgenis")));
  }

  @Test
  void testPartlyCustomRdf() throws IOException {
    final Set<Namespace> expectedNamespaces =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "RdfPartlyCustom1", "http://localhost:8080/RdfPartlyCustom1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "RdfPartlyCustom2", "http://localhost:8080/RdfPartlyCustom2/api/rdf/"));
            add(new SimpleNamespace("ncit", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final String customRdf1 =
        """
    @prefix ncit: <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#> .
    """;

    var handler = new InMemoryRDFHandler() {};
    validateNamespaces(handler, "RdfPartlyCustom", expectedNamespaces, customRdf1, null);
  }

  @Test
  void testCustomOrEmptyRdf() throws IOException {
    final Set<Namespace> expectedNamespaces =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "RdfcustomOrEmpty1", "http://localhost:8080/RdfcustomOrEmpty1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "RdfcustomOrEmpty2", "http://localhost:8080/RdfcustomOrEmpty2/api/rdf/"));
            add(new SimpleNamespace("ncit", "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#"));
          }
        };

    final String customRdf1 =
        """
    @prefix ncit: <http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#> .
    """;

    final String customRdf2 = "";

    var handler = new InMemoryRDFHandler() {};
    validateNamespaces(handler, "RdfcustomOrEmpty", expectedNamespaces, customRdf1, customRdf2);
  }

  @Test
  void testFileMetadataTriples() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(fileTest), handler);

    Set<Value> files =
        handler
            .resources
            .get(Values.iri("http://localhost:8080/fileTest/api/rdf/MyFiles?id=1"))
            .get(Values.iri("http://localhost:8080/fileTest/api/rdf/MyFiles/column/file"));

    IRI fileIRI = (IRI) files.stream().findFirst().get();

    Set<Value> fileNames = handler.resources.get(fileIRI).get(DCTERMS.TITLE);
    Set<Value> fileFormats = handler.resources.get(fileIRI).get(DCTERMS.FORMAT);

    assertAll(
        () -> assertEquals(1, files.size()),
        () -> assertEquals(1, fileNames.size()),
        () -> assertEquals(Values.literal("molgenis.png"), fileNames.stream().findFirst().get()),
        () -> assertEquals(1, fileFormats.size()),
        () ->
            assertEquals(
                Values.iri("http://www.iana.org/assignments/media-types/image/png"),
                fileFormats.stream().findFirst().get()));
  }

  @Test
  void refBackInRdf() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(refBackTest), handler);

    Set<Value> refBacks =
        handler
            .resources
            .get(Values.iri("http://localhost:8080/refBackTest/api/rdf/TableRefBack?id=a"))
            .get(
                Values.iri(
                    "http://localhost:8080/refBackTest/api/rdf/TableRefBack/column/backlink"));
    assertEquals(
        Set.of(Values.iri("http://localhost:8080/refBackTest/api/rdf/TableRef?id=1")), refBacks);
  }

  /**
   * Helper test method to compare namespaces of 2 schemas.
   *
   * @param handler handler to be used
   * @param schemaTestprefix prefix for created schemas ("1" & "2" is added to this for the 2
   *     different schemes)
   * @param expectedNamespaces set containing the expected combined namespaces
   * @param customRdf1 custom_rdf setting field for first schema
   * @param customRdf2 custom_rdf setting field for first schema (or null if it should not be set)
   * @throws IOException
   */
  private void validateNamespaces(
      InMemoryRDFHandler handler,
      String schemaTestprefix,
      Set<Namespace> expectedNamespaces,
      String customRdf1,
      String customRdf2)
      throws IOException {
    var schema1 = database.dropCreateSchema(schemaTestprefix + "1");
    var schema2 = database.dropCreateSchema(schemaTestprefix + "2");
    schema1.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf1);
    if (customRdf2 != null) {
      schema2.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf2);
    }
    getAndParseRDF(Selection.of(schema1, schema2), handler);
    assertEquals(expectedNamespaces, handler.namespaces);
  }

  /**
   * Helper method to reduce boilerplate code in the tests.<br>
   * <b>Note</b> this method delegates to the handler for the results of parsing.
   *
   * @param selection the schemas to request and parse
   * @param handler the handler for parsing the RDF.
   * @throws IOException when failing to parse
   */
  private void getAndParseRDF(Selection selection, RDFHandler handler) throws IOException {
    OutputStream outputStream = new ByteArrayOutputStream();
    var rdf = new RDFService("http://localhost:8080", RDF_API_LOCATION, null);
    rdf.describeAsRDF(
        outputStream, selection.table, selection.rowId, selection.columnName, selection.schemas);
    String result = outputStream.toString();
    var parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(handler);
    parser.parse(new StringReader(result));
  }

  static class Selection {
    Schema[] schemas;
    Table table;
    String rowId;
    String columnName;

    static Selection of(Schema... schemas) {
      var selection = new Selection();
      selection.schemas = schemas;
      return selection;
    }

    static Selection of(Schema schema, String table) {
      var selection = Selection.of(schema);
      selection.table = schema.getTable(table);
      return selection;
    }

    static Selection of(Schema[] schema, Table table) {
      var selection = Selection.of(schema);
      selection.table = table;
      return selection;
    }

    static Selection of(Schema schema, String table, String columnName) {
      var selection = Selection.of(schema, table);
      selection.columnName = columnName;
      return selection;
    }

    static Selection ofRow(Schema schema, String table, String rowId) {
      var selection = Selection.of(schema, table);
      selection.rowId = rowId;
      return selection;
    }

    static Selection ofRow(Schema[] schema, Table table, String rowId) {
      var selection = Selection.of(schema, table);
      selection.rowId = rowId;
      return selection;
    }
  }

  void assertPresence(InMemoryRDFHandler handler, Map<ValidationTriple, Boolean> presenceMap) {
    // Tracks errors.
    List<String> errors = new ArrayList<>();

    for (ValidationTriple triple : presenceMap.keySet()) {
      Map<IRI, Set<Value>> predicates = handler.resources.get(triple.getSubject());
      // If Triple should be present in handler.
      if (presenceMap.get(triple)) {
        if (predicates == null) {
          errors.add("Missing predicates for subject: " + triple.getSubject());
          continue;
        }
        Set<Value> objects = predicates.get(triple.getPredicate());
        if (objects == null) {
          errors.add("Missing objects for predicate: " + triple.getPredicate());
          continue;
        }
        if (objects.size() != 1) {
          errors.add("Only 1 object should be present for: " + triple.getPredicate());
        }
        Object firstObject = objects.toArray()[0];
        if (!triple.getObject().equals(firstObject)) {
          errors.add(
              "First object not equal to expected value. Found \""
                  + firstObject
                  + "\", but should be \""
                  + triple.getObject()
                  + "\"");
        }
      } // If Triple should not be present in handler.
      else {
        if (predicates != null)
          errors.add("Found predicates while expecting none for subject: " + triple.getSubject());
      }
    }

    // Compares error ArrayList to empty one so actual messages are shown if any are found.
    assertEquals(new ArrayList<>(), errors);
  }

  private enum ValidationTriple {
    ID1(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=1",
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root/column/rootColumn",
        "id1 data"),
    ID2(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=2",
        "http://localhost:8080/tableInheritanceTest/api/rdf/Child/column/childColumn",
        "id2 data"),
    ID3(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=3",
        "http://localhost:8080/tableInheritanceTest/api/rdf/GrandchildTypeA/column/grandchildColumn",
        "id3 data"),
    ID4(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=4",
        "http://localhost:8080/tableInheritanceTest/api/rdf/GrandchildTypeB/column/grandchildColumn",
        "id4 data"),
    ID4_GRANDPARENT_FIELD(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=4",
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root/column/rootColumn",
        "id4 data for rootColumn"),
    ID4_PARENT_FIELD(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=4",
        "http://localhost:8080/tableInheritanceTest/api/rdf/Child/column/childColumn",
        "id4 data for childColumn"),
    ID5(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=5",
        "http://localhost:8080/tableInheritanceExternalSchemaTest/api/rdf/ExternalChild/column/externalChildColumn",
        "id5 data"),
    ID6(
        "http://localhost:8080/tableInheritanceTest/api/rdf/Root?id=6",
        "http://localhost:8080/tableInheritanceExternalSchemaTest/api/rdf/ExternalGrandchild/column/externalGrandchildColumn",
        "id6 data"),
    UNRELATED(
        "http://localhost:8080/tableInheritanceExternalSchemaTest/api/rdf/ExternalUnrelated?id=a",
        "http://localhost:8080/tableInheritanceExternalSchemaTest/api/rdf/ExternalUnrelated/column/externalUnrelatedColumn",
        "unrelated data");

    private final Triple triple;

    public Resource getSubject() {
      return triple.getSubject();
    }

    public IRI getPredicate() {
      return triple.getPredicate();
    }

    public Value getObject() {
      return triple.getObject();
    }

    ValidationTriple(String subjectUrl, String predicateUrl, String objectString) {
      this.triple =
          SimpleValueFactory.getInstance()
              .createTriple(
                  Values.iri(subjectUrl), Values.iri(predicateUrl), Values.literal(objectString));
    }
  }
}
