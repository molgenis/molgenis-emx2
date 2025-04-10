package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.rdf.RDFTest.ValidationSubjects.COMP_CHILD1_FIRST;
import static org.molgenis.emx2.rdf.RDFTest.ValidationSubjects.COMP_CHILD1_SECOND;
import static org.molgenis.emx2.rdf.RDFTest.ValidationSubjects.COMP_GRANDCHILD1_FIRST;
import static org.molgenis.emx2.rdf.RDFTest.ValidationSubjects.COMP_GRANDCHILD1_SECOND;
import static org.molgenis.emx2.rdf.RDFTest.ValidationSubjects.COMP_ROOT1_FIRST;
import static org.molgenis.emx2.rdf.RDFTest.ValidationSubjects.COMP_ROOT2_FIRST;
import static org.molgenis.emx2.rdf.RdfUtils.SETTING_CUSTOM_RDF;
import static org.molgenis.emx2.rdf.RdfUtils.SETTING_SEMANTIC_PREFIXES;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.molgenis.emx2.datamodels.DataModels;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RDFTest {
  /**
   * Encoded id for the Pet pooky. The id string is composed by base64 encoding the id columns and
   * their values separately. Column names and values are separated by an ampersand and multiple
   * column / value pairs by a semicolon. Colums are sorted alphabetically for a stable order.
   */
  public static final String POOKY_ROWID = "name=pooky";

  static final String BASE_URL = "http://localhost:8080";
  static final String RDF_API_LOCATION = "/api/rdf";

  static final ClassLoader classLoader = ColumnTypeRdfMapperTest.class.getClassLoader();

  static Database database;
  static List<Schema> petStoreSchemas;
  static Schema petStore_nr1;
  static Schema petStore_nr2;
  static Schema compositeKeyTest;
  static Schema ontologyTest;
  static Schema ontologyCrossSchemaTest;
  static Schema tableInherTest;
  static Schema tableInherExtTest;
  static Schema fileTest;
  static Schema refBackTest;
  static Schema refLinkTest;
  static Schema semanticTest;

  final Set<Namespace> DEFAULT_NAMESPACES =
      DefaultNamespace.streamAll().collect(Collectors.toSet());

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    petStore_nr1 = database.dropCreateSchema("petStoreNr1");
    petStore_nr2 = database.dropCreateSchema("petStoreNr2");
    DataModels.Profile.PET_STORE.getImportTask(petStore_nr1, true).run();
    DataModels.Profile.PET_STORE.getImportTask(petStore_nr2, true).run();
    petStoreSchemas = List.of(petStore_nr1, petStore_nr2);

    // Test schema for composite keys
    compositeKeyTest = database.dropCreateSchema(RDFTest.class.getSimpleName() + "_compositeKey");
    compositeKeyTest.create(
        table("root1", column("r1").setType(ColumnType.REF).setRefTable("child1").setPkey()),
        table(
            "root2",
            column("r2a").setPkey(),
            column("r2b").setType(ColumnType.REF).setRefTable("child1").setPkey()),
        table(
            "child1",
            column("c1a").setPkey(),
            column("c1b").setType(ColumnType.REF).setRefTable("grandchild1").setPkey(),
            column("grandchild1ref").setType(ColumnType.REF).setRefTable("grandchild1"),
            column("root1refback")
                .setType(ColumnType.REFBACK)
                .setRefTable("root1")
                .setRefBack("r1"),
            column("root2refback")
                .setType(ColumnType.REFBACK)
                .setRefTable("root2")
                .setRefBack("r2b")),
        table(
            "grandchild1",
            column("gc1a").setPkey(),
            column("gc1b").setPkey(),
            column("child1refback")
                .setType(ColumnType.REFBACK)
                .setRefTable("child1")
                .setRefBack("c1b")));

    compositeKeyTest
        .getTable("grandchild1")
        .insert(
            row("gc1a", "gc1a_first", "gc1b", "gc1b_first"),
            row("gc1a", "gc1a_second", "gc1b", "gc1b_second"));

    compositeKeyTest
        .getTable("child1")
        .insert(
            row("c1a", "c1a_first", "c1b.gc1a", "gc1a_first", "c1b.gc1b", "gc1b_first"),
            row(
                "c1a",
                "c1a_second",
                "c1b.gc1a",
                "gc1a_first",
                "c1b.gc1b",
                "gc1b_first",
                "grandchild1ref.gc1a",
                "gc1a_second",
                "grandchild1ref.gc1b",
                "gc1b_second"));

    compositeKeyTest
        .getTable("root1")
        .insert(
            row("r1.c1a", "c1a_first", "r1.c1b.gc1a", "gc1a_first", "r1.c1b.gc1b", "gc1b_first"));

    compositeKeyTest
        .getTable("root2")
        .insert(
            row(
                "r2a",
                "r2a_first",
                "r2b.c1a",
                "c1a_second",
                "r2b.c1b.gc1a",
                "gc1a_first",
                "r2b.c1b.gc1b",
                "gc1b_first"));

    // Test schema for ontologies
    database.dropSchemaIfExists(
        RDFTest.class.getSimpleName() + "_ontology_cross_schema"); // in case tearDown fails
    ontologyTest = database.dropCreateSchema(RDFTest.class.getSimpleName() + "_ontology");
    ontologyTest.create(table("Diseases").setTableType(TableType.ONTOLOGIES));
    ontologyTest.create(
        table(
            "Patients",
            column("name").setPkey(),
            column("diseases")
                .setSemantics("http://purl.obolibrary.org/obo/NCIT_C2991")
                .setType(ColumnType.ONTOLOGY_ARRAY)
                .setRefTable("Diseases")));

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

    ontologyTest
        .getTable("Patients")
        .insert(
            row(
                "name",
                "bob",
                "diseases",
                "\"U07\", \"C00-C14 Malignant neoplasms of lip, oral cavity and pharynx\""));

    // Test for cross-schema references
    ontologyCrossSchemaTest =
        database.dropCreateSchema(RDFTest.class.getSimpleName() + "_ontology_cross_schema");
    ontologyCrossSchemaTest.create(
        table(
            "Patients",
            column("name").setPkey(),
            column("diseases")
                .setSemantics("http://purl.obolibrary.org/obo/NCIT_C2991")
                .setType(ColumnType.ONTOLOGY_ARRAY)
                .setRefSchemaName(RDFTest.class.getSimpleName() + "_ontology")
                .setRefTable("Diseases")));

    ontologyCrossSchemaTest
        .getTable("Patients")
        .insert(
            row(
                "name",
                "pim",
                "diseases",
                "\"U07\", \"C00-C14 Malignant neoplasms of lip, oral cavity and pharynx\""));

    // Test table inheritance
    // Use example from the catalogue schema since this has all the different issues.
    database.dropSchemaIfExists("tableInheritanceExternalSchemaTest"); // in case tearDown fails
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

    // refLink test
    refLinkTest = database.dropCreateSchema("refLinkTest");

    refLinkTest.create(
        table("table1", column("id").setType(ColumnType.STRING).setPkey()),
        table(
            "table2",
            column("id1").setPkey().setType(ColumnType.REF).setRefTable("table1"),
            column("id2").setType(ColumnType.STRING).setPkey()),
        table(
            "table3",
            column("p1").setPkey().setType(ColumnType.REF).setRefTable("table1"),
            column("p2").setPkey().setType(ColumnType.REF).setRefTable("table2").setRefLink("p1"),
            column("ref").setType(ColumnType.REF).setRefTable("table2").setRefLink("p1")));

    refLinkTest.getTable("table1").insert(row("id", "t1First"));
    refLinkTest.getTable("table2").insert(row("id1", "t1First", "id2", "t2First"));
    refLinkTest.getTable("table3").insert(row("p1", "t1First", "p2", "t2First"));

    // semantic test
    semanticTest = database.dropCreateSchema("semanticTest");

    semanticTest.create(
        table(
            "valid",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("title")
                .setType(ColumnType.STRING)
                .setSemantics("http://purl.org/dc/terms/title"),
            column("description").setType(ColumnType.STRING).setSemantics("dcterms:description"),
            column("nonDefinedPrefix")
                .setType(ColumnType.STRING)
                .setSemantics("nonDefinedPrefix:value")),
        table(
            "invalid",
            column("id").setType(ColumnType.STRING).setPkey(),
            column("theme").setType(ColumnType.STRING).setSemantics("theme")));

    semanticTest
        .getTable("valid")
        .insert(
            row("id", "1", "title", "test", "description", "test2", "nonDefinedPrefix", "test3"));
    semanticTest.getTable("invalid").insert(row("id", "2", "theme", "test4"));

    semanticTest
        .getMetadata()
        .setSetting(SETTING_SEMANTIC_PREFIXES, "dcterms,http://purl.org/dc/terms/");
  }

  private static String getApi(Schema schema, boolean trailingSlash) {
    return BASE_URL + "/" + schema.getName() + RDF_API_LOCATION + (trailingSlash ? "/" : "");
  }

  /**
   * Actual API has no trailing slash. Use {@link #getApi(Schema, boolean)} with `false` if needed.
   * @param schema
   * @return
   */
  private static String getApi(Schema schema) {
    return getApi(schema, true);
  }

  @AfterAll
  public static void tearDown() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchema(petStore_nr1.getName());
    database.dropSchema(petStore_nr2.getName());
    database.dropSchema(compositeKeyTest.getName());
    database.dropSchema(ontologyCrossSchemaTest.getName());
    database.dropSchema(ontologyTest.getName());
    database.dropSchema(tableInherExtTest.getName());
    database.dropSchema(tableInherTest.getName());
    database.dropSchema(fileTest.getName());
    database.dropSchema(refBackTest.getName());
    database.dropSchema(refLinkTest.getName());
    database.dropSchema(semanticTest.getName());
  }

  @Test
  void testThatColumnsAreAProperty() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1), handler);

    assertFalse(handler.resources.entrySet().isEmpty());
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

    assertFalse(handler.resources.entrySet().isEmpty());
    for (var resource : handler.resources.entrySet()) {
      var subClasses = resource.getValue().get(RDFS.SUBCLASSOF);
      if (subClasses != null && subClasses.contains(BasicIRI.SIO_DATABASE_TABLE)) {
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

    assertFalse(handler.resources.entrySet().isEmpty());
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

    assertFalse(handler.resources.entrySet().isEmpty());
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

    assertFalse(handler.resources.keySet().isEmpty());
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
            BasicIRI.SIO_DATABASE);

    assertFalse(handler.resources.entrySet().isEmpty());
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
  void testCompositeKeysPresenceOnFullSchema() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(compositeKeyTest), handler);

    new RdfValidator()
        .add(ValidationTriple.COMP_ROOT1_KEY_REF.getTriple(), true)
        .add(ValidationTriple.COMP_ROOT2_KEY_REF.getTriple(), true)
        .add(ValidationTriple.COMP_CHILD1_FIRST_KEY_REF.getTriple(), true)
        .add(ValidationTriple.COMP_CHILD1_FIRST_REFBACK_ROOT1.getTriple(), true)
        .add(ValidationTriple.COMP_CHILD1_SECOND_KEY_REF.getTriple(), true)
        .add(ValidationTriple.COMP_CHILD1_SECOND_REFBACK_ROOT2.getTriple(), true)
        .add(ValidationTriple.COMP_CHILD1_SECOND_NON_KEY_REF.getTriple(), true)
        .add(ValidationTriple.COMP_GRANDCHILD_REFBACK_1.getTriple(), true)
        .add(ValidationTriple.COMP_GRANDCHILD_REFBACK_2.getTriple(), true)
        .validate(handler);
  }

  @Test
  void testCompositeKeysRowSelection() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(
        Selection.ofRow(
            compositeKeyTest, "Child1", "c1a=c1a_second&c1b.gc1a=gc1a_first&c1b.gc1b=gc1b_first"),
        handler);
    new RdfValidator()
        .add(ValidationTriple.COMP_ROOT1_KEY_REF.getTriple(), false)
        .add(ValidationTriple.COMP_ROOT2_KEY_REF.getTriple(), false)
        .add(ValidationTriple.COMP_CHILD1_FIRST_KEY_REF.getTriple(), false)
        .add(ValidationTriple.COMP_CHILD1_FIRST_REFBACK_ROOT1.getTriple(), false)
        .add(ValidationTriple.COMP_CHILD1_SECOND_KEY_REF.getTriple(), true)
        .add(ValidationTriple.COMP_CHILD1_SECOND_REFBACK_ROOT2.getTriple(), true)
        .add(ValidationTriple.COMP_CHILD1_SECOND_NON_KEY_REF.getTriple(), true)
        .add(ValidationTriple.COMP_GRANDCHILD_REFBACK_1.getTriple(), false)
        .add(ValidationTriple.COMP_GRANDCHILD_REFBACK_2.getTriple(), false)
        .validate(handler);
  }

  @Test
  void testThatInstancesUseReferToDatasetWithTheRightPredicate() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(petStore_nr1, "Pet", POOKY_ROWID), handler);

    assertFalse(handler.resources.keySet().isEmpty());
    for (var iri : handler.resources.keySet()) {
      // Select the triples for pooky
      if (iri.stringValue().endsWith(POOKY_ROWID)) {

        var pooky = handler.resources.get(iri);
        assertTrue(
            pooky.containsKey(BasicIRI.LD_DATASET_PREDICATE),
            "An instance of a Pet should refer back to the Collection using qb:dataSet");
        assertFalse(pooky.containsKey(BasicIRI.LD_DATASET_CLASS), "qb:DataSet is not a predicate");
      }
    }
  }

  @Test
  void testThatColumnPredicatesAreNotSubClasses() throws IOException {
    var database_column = Values.iri("http://semanticscience.org/resource/SIO_000757");
    var measure_property = Values.iri("http://purl.org/linked-data/cube#MeasureProperty");
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(petStore_nr1, "Pet", "name"), handler);

    assertFalse(handler.resources.keySet().isEmpty());
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

    assertFalse(handler.resources.keySet().isEmpty());
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

    assertFalse(handler.resources.keySet().isEmpty());
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

    assertFalse(handler.resources.keySet().isEmpty());
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

    assertFalse(handler.resources.keySet().isEmpty());
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
            getApi(ontologyTest)
                + "Diseases?name=C00-C14+Malignant+neoplasms+of+lip%2C+oral+cavity+and+pharynx");

    var parents = handler.resources.get(subject).get(RDFS.SUBCLASSOF);
    assertEquals(
        2, parents.size(), "This disease should only be a subclass of Diseases and C00-C75");
  }

  @Test
  void testDataTableOntologyColumnValue() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(ontologyTest, "Patients"), handler);

    Set<Value> expectedSemantic =
        Set.of(
            Values.iri("https://icd.who.int/browse10/2019/en#/U07"),
            Values.iri(
                getApi(ontologyTest)
                    + "Diseases?name=C00-C14+Malignant+neoplasms+of+lip%2C+oral+cavity+and+pharynx"));
    Set<Value> expectedNonSemantic =
        Set.of(
            Values.iri(getApi(ontologyTest) + "Diseases?name=U07"),
            Values.iri(
                getApi(ontologyTest)
                    + "Diseases?name=C00-C14+Malignant+neoplasms+of+lip%2C+oral+cavity+and+pharynx"));

    Set<Value> actualSemantic =
        handler
            .resources
            .get(Values.iri(getApi(ontologyTest) + "Patients?name=bob"))
            .get(Values.iri("http://purl.obolibrary.org/obo/NCIT_C2991"));
    Set<Value> actualNonSemantic =
        handler
            .resources
            .get(Values.iri(getApi(ontologyTest) + "Patients?name=bob"))
            .get(Values.iri(getApi(ontologyTest) + "Patients/column/diseases"));

    assertEquals(expectedSemantic, actualSemantic);
    assertEquals(expectedNonSemantic, actualNonSemantic);
  }

  @Test
  void testCrossSchemaDataTableOntologyColumnValue() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(ontologyCrossSchemaTest, "Patients"), handler);

    Set<Value> expectedSemantic =
        Set.of(
            Values.iri("https://icd.who.int/browse10/2019/en#/U07"),
            Values.iri(
                getApi(ontologyTest)
                    + "Diseases?name=C00-C14+Malignant+neoplasms+of+lip%2C+oral+cavity+and+pharynx"));
    Set<Value> expectedNonSemantic =
        Set.of(
            Values.iri(getApi(ontologyTest) + "Diseases?name=U07"),
            Values.iri(
                getApi(ontologyTest)
                    + "Diseases?name=C00-C14+Malignant+neoplasms+of+lip%2C+oral+cavity+and+pharynx"));

    Set<Value> actualSemantic =
        handler
            .resources
            .get(Values.iri(getApi(ontologyCrossSchemaTest) + "Patients?name=pim"))
            .get(Values.iri("http://purl.obolibrary.org/obo/NCIT_C2991"));
    Set<Value> actualNonSemantic =
        handler
            .resources
            .get(Values.iri(getApi(ontologyCrossSchemaTest) + "Patients?name=pim"))
            .get(Values.iri(getApi(ontologyCrossSchemaTest) + "Patients/column/diseases"));

    assertEquals(expectedSemantic, actualSemantic);
    assertEquals(expectedNonSemantic, actualNonSemantic);
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
                    Values.iri(getApi(tableInherTest) + "Root/column/rootColumn")),
                "There should be a predicate for the rootColumn in the Root table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(tableInherTest) + "Child/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the Child table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(tableInherTest) + "GrandchildTypeA/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the GrandchildTypeA table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(tableInherTest) + "GrandchildTypeB/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the GrandchildTypeB table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(tableInherTest) + "ExternalChild/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the ExternalChild table"),
        () ->
            assertFalse(
                handler.resources.containsKey(
                    Values.iri(getApi(tableInherTest) + "ExternalGrandchild/column/rootColumn")),
                "There should not be a predicate for the rootColumn in the ExternalGrandchild table"));
  }

  @Test
  void testTableInheritanceRetrieveData() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), true)
        .add(ValidationTriple.INHER_ID2.getTriple(), true)
        .add(ValidationTriple.INHER_ID3.getTriple(), true)
        .add(ValidationTriple.INHER_ID4.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID5.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableRoot() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "Root"), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), true)
        .add(ValidationTriple.INHER_ID2.getTriple(), true)
        .add(ValidationTriple.INHER_ID3.getTriple(), true)
        .add(ValidationTriple.INHER_ID4.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID5.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableChild() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "Child"), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // parent of selected table
        .add(ValidationTriple.INHER_ID2.getTriple(), true)
        .add(ValidationTriple.INHER_ID3.getTriple(), true) // child
        .add(ValidationTriple.INHER_ID4.getTriple(), true) // child
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true) // child
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true) // child
        .add(ValidationTriple.INHER_ID5.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableGrandchildTypeA() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "GrandchildTypeA"), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // grandparent of selected table
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // parent of selected table
        .add(ValidationTriple.INHER_ID3.getTriple(), true)
        .add(ValidationTriple.INHER_ID4.getTriple(), false) // sibling of selected table
        .add(
            ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), false) // sibling of selected table
        .add(
            ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(),
            false) // sibling of selected table
        .add(ValidationTriple.INHER_ID5.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithTableGrandchildTypeB() throws IOException {
    // All subjects still use Root IRIs but offers a way to "filter out parent triples".
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, "GrandchildTypeB"), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // grandparent of selected table
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // parent of selected table
        .add(ValidationTriple.INHER_ID3.getTriple(), false) // sibling of selected table
        .add(ValidationTriple.INHER_ID4.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID5.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // different schema
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataWithRowId() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(tableInherTest, "Root", "id=4"), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID3.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID5.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // not selected
        .validate(handler);
  }

  @Test
  void testTableInheritanceExternalSchemaRetrieveData() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherExtTest), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID3.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID4.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID5.getTriple(), true)
        .add(ValidationTriple.INHER_ID6.getTriple(), true)
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), true)
        .validate(handler);
  }

  @Test
  void testTableInheritanceExternalSchemaDataWithTableExternalChild() throws IOException {
    // Note that even though the subject has an ID IRI based on table Root, this table is not part
    // of the selected scheme so this table cannot be selected:
    // `tableInherExtTest.getTable("Root")` == `null`
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherExtTest, "ExternalChild"), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID3.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID4.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), false) // different schema
        .add(ValidationTriple.INHER_ID5.getTriple(), true)
        .add(ValidationTriple.INHER_ID6.getTriple(), true)
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // not part of inheritance
        .validate(handler);
  }

  @Test
  void testTableInheritanceExternalSchemaDataWithRowId() throws IOException {
    // Note that even though the subject has an ID IRI based on table Root, this table is not part
    // of the selected scheme so this table cannot be selected:
    // `tableInherExtTest.getTable("Root")` == `null`
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(tableInherExtTest, "ExternalChild", "id=5"), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID3.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID5.getTriple(), true)
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // not selected
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataMultiSchema() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(tableInherTest, tableInherExtTest), handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), true)
        .add(ValidationTriple.INHER_ID2.getTriple(), true)
        .add(ValidationTriple.INHER_ID3.getTriple(), true)
        .add(ValidationTriple.INHER_ID4.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID5.getTriple(), true)
        .add(ValidationTriple.INHER_ID6.getTriple(), true)
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), true)
        .validate(handler);
  }

  @Test
  void testTableInheritanceRetrieveDataMultiSchemaWithTableRoot() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(
        Selection.of(
            new Schema[] {tableInherTest, tableInherExtTest}, tableInherTest.getTable("Root")),
        handler);
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), true)
        .add(ValidationTriple.INHER_ID2.getTriple(), true)
        .add(ValidationTriple.INHER_ID3.getTriple(), true)
        .add(ValidationTriple.INHER_ID4.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID5.getTriple(), true)
        .add(ValidationTriple.INHER_ID6.getTriple(), true)
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // not part of inheritance
        .validate(handler);
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
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID3.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), true)
        .add(ValidationTriple.INHER_ID5.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // not selected
        .validate(handler);
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
    new RdfValidator()
        .add(ValidationTriple.INHER_ID1.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID2.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID3.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4_PARENT_FIELD.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID4_GRANDPARENT_FIELD.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_ID5.getTriple(), true)
        .add(ValidationTriple.INHER_ID6.getTriple(), false) // not selected
        .add(ValidationTriple.INHER_UNRELATED.getTriple(), false) // not selected
        .validate(handler);
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

    assertFalse(handler.resources.keySet().isEmpty());
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

    assertFalse(handler.resources.keySet().isEmpty());
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
    var rootIRI = Values.iri(getApi(schema) + root.getIdentifier());
    var childIRI = Values.iri(getApi(schema) + child.getIdentifier());
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
    var rootIRI = Values.iri(getApi(schema) + root.getIdentifier());
    var childIRI = Values.iri(getApi(schema) + child.getIdentifier());
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
            add(new SimpleNamespace("CustomRdfEdit", BASE_URL + "/CustomRdfEdit/api/rdf/"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final String customRdf =
        """
@prefix example: <http://example.com/> .
<https://molgenis.org/> example:test "Molgenis" .
""";

    try {
      Schema schema = database.dropCreateSchema("CustomRdfEdit");
      // Test default behaviour.
      assertFalse(schema.hasSetting(SETTING_CUSTOM_RDF));
      var handlerBefore = new InMemoryRDFHandler() {};
      getAndParseRDF(Selection.of(schema), handlerBefore);
      assertFalse(handlerBefore.resources.containsKey(Values.iri("https://molgenis.org/")));

      // Change setting
      schema.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);

      // Test behaviour after changing setting.
      var handlerAfter = new InMemoryRDFHandler() {};
      getAndParseRDF(Selection.of(schema), handlerAfter);
      assertEquals(
          defaultNamespaces, handlerAfter.namespaces); // example prefix should NOT be present
      assertTrue(
          handlerAfter
              .resources
              .get(Values.iri("https://molgenis.org/"))
              .get(Values.iri("http://example.com/test"))
              .contains(Values.literal("Molgenis")));

    } finally {
      database.dropSchemaIfExists("CustomRdfEdit");
    }
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

    try {
      Schema schema = database.dropCreateSchema("CustomRdfInvalid");
      schema.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);
      var handler = new InMemoryRDFHandler() {};
      assertThrows(MolgenisException.class, () -> getAndParseRDF(Selection.of(schema), handler));
    } finally {
      database.dropSchemaIfExists("CustomRdfInvalid");
    }
  }

  @Test
  void testEmptyCustomRdfSetting() throws IOException {
    final String customRdf = "";

    try {
      Schema schema = database.dropCreateSchema("CustomRdfEmpty");
      schema.getMetadata().setSetting(SETTING_CUSTOM_RDF, customRdf);
      var handler = new InMemoryRDFHandler() {};
      assertDoesNotThrow(() -> getAndParseRDF(Selection.of(schema), handler));
    } finally {
      database.dropSchemaIfExists("CustomRdfEmpty");
    }
  }

  @Test
  void testSemanticPrefixesSetting() throws IOException {
    final Set<Namespace> defaultNamespaces =
        new HashSet<>() {
          {
            add(new SimpleNamespace("PrefixesEdit", BASE_URL + "/PrefixesEdit/api/rdf/"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final Set<Namespace> customNamespaces =
        new HashSet<>() {
          {
            add(new SimpleNamespace("PrefixesEdit", BASE_URL + "/PrefixesEdit/api/rdf/"));
            add(new SimpleNamespace("dcat", "http://www.w3.org/ns/dcat#"));
            add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
          }
        };

    final String customPrefixes =
        """
    dcat,http://www.w3.org/ns/dcat#
    dcterms,http://purl.org/dc/terms/
    """;

    try {
      Schema schema = database.dropCreateSchema("PrefixesEdit");
      // Test default behaviour.
      assertFalse(schema.hasSetting(SETTING_SEMANTIC_PREFIXES));
      var handlerBefore = new InMemoryRDFHandler() {};
      getAndParseRDF(Selection.of(schema), handlerBefore);
      assertEquals(defaultNamespaces, handlerBefore.namespaces);

      // Change setting
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);

      // Test behaviour after changing setting.
      var handlerAfter = new InMemoryRDFHandler() {};
      getAndParseRDF(Selection.of(schema), handlerAfter);
      assertEquals(customNamespaces, handlerAfter.namespaces);
    } finally {
      database.dropSchemaIfExists("PrefixesEdit");
    }
  }

  @Test
  void testMissingIriSemanticPrefixesSetting() throws IOException {
    final String customPrefixes = "example,example";

    try {
      Schema schema = database.dropCreateSchema("PrefixesMissingIri");
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);
      var handler = new InMemoryRDFHandler() {};
      assertThrows(MolgenisException.class, () -> getAndParseRDF(Selection.of(schema), handler));
    } finally {
      database.dropSchemaIfExists("PrefixesMissingIri");
    }
  }

  @Test
  void testIllegalPrefixSemanticPrefixesSetting() throws IOException {
    final String customPrefixes = "urn,http://example.com";

    try {
      Schema schema = database.dropCreateSchema("PrefixesIllegalPrefix");
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);
      var handler = new InMemoryRDFHandler() {};
      assertThrows(MolgenisException.class, () -> getAndParseRDF(Selection.of(schema), handler));
    } finally {
      database.dropSchemaIfExists("PrefixesIllegalPrefix");
    }
  }

  @Test
  void testEmptySemanticPrefixesSetting() throws IOException {
    final Set<Namespace> expectedNamespaces =
        new HashSet<>() {
          {
            add(new SimpleNamespace("PrefixesEmpty", BASE_URL + "/PrefixesEmpty/api/rdf/"));
          }
        };

    final String customPrefixes = "";

    try {
      Schema schema = database.dropCreateSchema("PrefixesEmpty");
      schema.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes);
      var handler = new InMemoryRDFHandler() {};
      getAndParseRDF(Selection.of(schema), handler);
      assertEquals(expectedNamespaces, handler.namespaces);
    } finally {
      database.dropSchemaIfExists("PrefixesEmpty");
    }
  }

  @Test
  void testDuplicateNamespaces() throws IOException {
    final Set<Namespace> expectedNamespace =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "PrefixSettingEqual1", BASE_URL + "/PrefixSettingEqual1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingEqual2", BASE_URL + "/PrefixSettingEqual2/api/rdf/"));
            add(new SimpleNamespace("dcterms", "http://purl.org/dc/terms/"));
          }
        };

    final String customPrefixes1 =
        """
dcterms,http://purl.org/dc/terms/
""";

    final String customPrefixes2 =
        """
dcterms,http://purl.org/dc/terms/
""";

    validateNamespaces("PrefixSettingEqual", expectedNamespace, customPrefixes1, customPrefixes2);
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
            add(
                new SimpleNamespace(
                    "PrefixSettingName1", BASE_URL + "/PrefixSettingName1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingName2", BASE_URL + "/PrefixSettingName2/api/rdf/"));
            add(new SimpleNamespace("dcterms1", "http://purl.org/dc/terms/"));
          }
        };

    final String customPrefixes1 =
        """
dcterms1,http://purl.org/dc/terms/
""";

    final String customPrefixes2 =
        """
dcterms2,http://purl.org/dc/terms/
""";

    validateNamespaces("PrefixSettingName", expectedNamespace, customPrefixes1, customPrefixes2);
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
                    "PrefixSettingNameIri1", BASE_URL + "/PrefixSettingNameIri1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingNameIri2", BASE_URL + "/PrefixSettingNameIri2/api/rdf/"));
            add(new SimpleNamespace("name", "http://www.w3.org/2000/01/rdf-schema#"));
          }
        };

    final String customPrefixes1 =
        """
name,http://purl.org/dc/terms/
    """;

    final String customPrefixes2 =
        """
name,http://www.w3.org/2000/01/rdf-schema#
    """;

    validateNamespaces("PrefixSettingNameIri", expectedNamespace, customPrefixes1, customPrefixes2);
  }

  @Test
  void testSingleCustomPrefixesSetting() throws IOException {
    final Set<Namespace> expectedNamespaces =
        new HashSet<>() {
          {
            add(
                new SimpleNamespace(
                    "PrefixSettingPartly1", BASE_URL + "/PrefixSettingPartly1/api/rdf/"));
            add(
                new SimpleNamespace(
                    "PrefixSettingPartly2", BASE_URL + "/PrefixSettingPartly2/api/rdf/"));
            add(new SimpleNamespace("example", "http://example.com/"));
            addAll(DEFAULT_NAMESPACES);
          }
        };

    final String customPrefixes1 =
        """
example,http://example.com/
    """;

    validateNamespaces("PrefixSettingPartly", expectedNamespaces, customPrefixes1, null);
  }

  @Test
  void testFileMetadataTriples() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(fileTest), handler);

    Set<Value> files =
        handler
            .resources
            .get(Values.iri(getApi(fileTest) + "MyFiles?id=1"))
            .get(Values.iri(getApi(fileTest) + "MyFiles/column/file"));

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
            .get(Values.iri(getApi(refBackTest) + "TableRefBack?id=a"))
            .get(Values.iri(getApi(refBackTest) + "TableRefBack/column/backlink"));
    assertEquals(Set.of(Values.iri(getApi(refBackTest) + "TableRef?id=1")), refBacks);
  }

  @Test
  void testRefLinkWorks() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    assertDoesNotThrow(() -> getAndParseRDF(Selection.of(refLinkTest), handler));
  }

  @Test
  void prefixedNames() throws IOException {
    Set<IRI> expectedPredicates =
        Set.of(
            Values.iri("http://purl.org/dc/terms/title"),
            Values.iri("http://purl.org/dc/terms/description"));

    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.of(semanticTest, "valid"), handler);
    Set<IRI> actualPredicates =
        handler.resources.get(Values.iri(getApi(semanticTest) + "Valid?id=1")).keySet();
    assertTrue(actualPredicates.containsAll(expectedPredicates));

    assertThrows(
        MolgenisException.class,
        () -> getAndParseRDF(Selection.of(semanticTest, "invalid"), handler));
  }

  /**
   * Helper test method to compare namespaces of 2 schemas.
   *
   * @param handler handler to be used
   * @param schemaTestprefix prefix for created schemas ("1" & "2" is added to this for the 2
   *     different schemes)
   * @param expectedNamespaces set containing the expected combined namespaces
   * @param customPrefixes1 custom_rdf setting field for first schema
   * @param customPrefixes2 custom_rdf setting field for first schema (or null if it should not be
   *     set)
   * @throws IOException
   */
  private void validateNamespaces(
      String schemaTestprefix,
      Set<Namespace> expectedNamespaces,
      String customPrefixes1,
      String customPrefixes2)
      throws IOException {
    try {
      var schema1 = database.dropCreateSchema(schemaTestprefix + "1");
      var schema2 = database.dropCreateSchema(schemaTestprefix + "2");
      schema1.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes1);
      if (customPrefixes2 != null) {
        schema2.getMetadata().setSetting(SETTING_SEMANTIC_PREFIXES, customPrefixes2);
      }

      var handler = new InMemoryRDFHandler() {};
      getAndParseRDF(Selection.of(schema1, schema2), handler);
      assertEquals(expectedNamespaces, handler.namespaces);
    } finally {
      database.dropSchemaIfExists(schemaTestprefix + "1");
      database.dropSchemaIfExists(schemaTestprefix + "2");
    }
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
    var rdf = new RDFService(BASE_URL, null);
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

  private enum ValidationTriple {
    // Inheritance testing
    INHER_ID1(
        getApi(tableInherTest) + "Root?id=1",
        getApi(tableInherTest) + "Root/column/rootColumn",
        Values.literal("id1 data")),
    INHER_ID2(
        getApi(tableInherTest) + "Root?id=2",
        getApi(tableInherTest) + "Child/column/childColumn",
        Values.literal("id2 data")),
    INHER_ID3(
        getApi(tableInherTest) + "Root?id=3",
        getApi(tableInherTest) + "GrandchildTypeA/column/grandchildColumn",
        Values.literal("id3 data")),
    INHER_ID4(
        getApi(tableInherTest) + "Root?id=4",
        getApi(tableInherTest) + "GrandchildTypeB/column/grandchildColumn",
        Values.literal("id4 data")),
    INHER_ID4_GRANDPARENT_FIELD(
        getApi(tableInherTest) + "Root?id=4",
        getApi(tableInherTest) + "Root/column/rootColumn",
        Values.literal("id4 data for rootColumn")),
    INHER_ID4_PARENT_FIELD(
        getApi(tableInherTest) + "Root?id=4",
        getApi(tableInherTest) + "Child/column/childColumn",
        Values.literal("id4 data for childColumn")),
    INHER_ID5(
        getApi(tableInherTest) + "Root?id=5",
        getApi(tableInherExtTest) + "ExternalChild/column/externalChildColumn",
        Values.literal("id5 data")),
    INHER_ID6(
        getApi(tableInherTest) + "Root?id=6",
        getApi(tableInherExtTest) + "ExternalGrandchild/column/externalGrandchildColumn",
        Values.literal("id6 data")),
    INHER_UNRELATED(
        getApi(tableInherExtTest) + "ExternalUnrelated?id=a",
        getApi(tableInherExtTest) + "ExternalUnrelated/column/externalUnrelatedColumn",
        Values.literal("unrelated data")),

    // Composite testing
    COMP_ROOT1_KEY_REF(
        COMP_ROOT1_FIRST.get(),
        getApi(compositeKeyTest) + "Root1/column/r1",
        COMP_CHILD1_FIRST.get()),
    COMP_ROOT2_KEY_REF(
        COMP_ROOT2_FIRST.get(),
        getApi(compositeKeyTest) + "Root2/column/r2b",
        COMP_CHILD1_SECOND.get()),
    COMP_CHILD1_FIRST_KEY_REF(
        COMP_CHILD1_FIRST.get(),
        getApi(compositeKeyTest) + "Child1/column/c1b",
        COMP_GRANDCHILD1_FIRST.get()),
    COMP_CHILD1_FIRST_REFBACK_ROOT1(
        COMP_CHILD1_FIRST.get(),
        getApi(compositeKeyTest) + "Child1/column/root1refback",
        COMP_ROOT1_FIRST.get()),
    COMP_CHILD1_SECOND_KEY_REF(
        COMP_CHILD1_SECOND.get(),
        getApi(compositeKeyTest) + "Child1/column/c1b",
        COMP_GRANDCHILD1_FIRST.get()),
    COMP_CHILD1_SECOND_REFBACK_ROOT2(
        COMP_CHILD1_SECOND.get(),
        getApi(compositeKeyTest) + "Child1/column/root2refback",
        COMP_ROOT2_FIRST.get()),
    COMP_CHILD1_SECOND_NON_KEY_REF(
        COMP_CHILD1_SECOND.get(),
        getApi(compositeKeyTest) + "Child1/column/grandchild1ref",
        COMP_GRANDCHILD1_SECOND.get()),
    COMP_GRANDCHILD_REFBACK_1(
        COMP_GRANDCHILD1_FIRST.get(),
        getApi(compositeKeyTest) + "Grandchild1/column/child1refback",
        COMP_CHILD1_FIRST.get()),
    COMP_GRANDCHILD_REFBACK_2(
        COMP_GRANDCHILD1_FIRST.get(),
        getApi(compositeKeyTest) + "Grandchild1/column/child1refback",
        COMP_CHILD1_FIRST.get());

    private final Triple triple;

    public Triple getTriple() {
      return triple;
    }

    ValidationTriple(String subjectUrl, String predicateUrl, Value object) {
      this(Values.iri(subjectUrl), predicateUrl, object);
    }

    ValidationTriple(Value subject, String predicateUrl, Value object) {
      this.triple =
          SimpleValueFactory.getInstance()
              .createTriple((Resource) subject, Values.iri(predicateUrl), object);
    }
  }

  enum ValidationSubjects {
    COMP_ROOT1_FIRST("Root1?r1.c1a=c1a_first&r1.c1b.gc1a=gc1a_first&r1.c1b.gc1b=gc1b_first"),
    COMP_ROOT2_FIRST(
        "Root2?r2a=r2a_first&r2b.c1a=c1a_second&r2b.c1b.gc1a=gc1a_first&r2b.c1b.gc1b=gc1b_first"),
    COMP_CHILD1_FIRST("Child1?c1a=c1a_first&c1b.gc1a=gc1a_first&c1b.gc1b=gc1b_first"),
    COMP_CHILD1_SECOND("Child1?c1a=c1a_second&c1b.gc1a=gc1a_first&c1b.gc1b=gc1b_first"),
    COMP_GRANDCHILD1_FIRST("Grandchild1?gc1a=gc1a_first&gc1b=gc1b_first"),
    COMP_GRANDCHILD1_SECOND("Grandchild1?gc1a=gc1a_second&gc1b=gc1b_second");

    Value value;

    public Value get() {
      return value;
    }

    ValidationSubjects(String resource) {
      this.value = Values.iri(getApi(compositeKeyTest) + resource);
    }
  }
}
