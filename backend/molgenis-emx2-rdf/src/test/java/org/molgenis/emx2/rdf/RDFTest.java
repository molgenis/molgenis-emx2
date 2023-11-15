package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandler;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RDFTest {

  static Database database;
  static List<Schema> petStoreSchemas;
  static final String RDF_API_LOCATION = "/api/rdf";
  static Schema petStore_nr1;
  static Schema petStore_nr2;
  static Schema compositeKeyTest;

  static RDFService rdf = new RDFService("http://localhost:8080", RDF_API_LOCATION, null);

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    petStore_nr1 = database.dropCreateSchema("petStoreNr1");
    petStore_nr2 = database.dropCreateSchema("petStoreNr2");
    PetStoreLoader petStoreLoader = new PetStoreLoader();
    petStoreLoader.load(petStore_nr1, true);
    petStoreLoader.load(petStore_nr2, true);
    petStoreSchemas = List.of(petStore_nr1, petStore_nr2);

    Database database = TestDatabaseFactory.getTestDatabase();
    compositeKeyTest = database.dropCreateSchema(RDFTest.class.getSimpleName());
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
  }

  @AfterAll
  public static void tearDown() {
    database = TestDatabaseFactory.getTestDatabase();
    database.dropSchema(petStore_nr1.getName());
    database.dropSchema(petStore_nr2.getName());
    database.dropSchema(compositeKeyTest.getName());
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
        "http://localhost:8080/RDFTest/api/rdf/Samples?patient.firstName=Donald&patient.lastName=Duck&id=sample1";
    var iris = handler.resources.keySet().stream().map(Objects::toString).toList();
    assertTrue(
        iris.contains(subjectWithCompositeKey),
        "A Sample resource should have a key based on patient.firstName, patient.lastName and id");
  }

  @Test
  void testThatRowCanBeFetchedByCompositeKey() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    var rowId = "patient.firstName=Donald&patient.lastName=Duck&id=sample1";
    getAndParseRDF(Selection.ofRow(compositeKeyTest, "Samples", rowId), handler);
    var subjectWithCompositeKey =
        "http://localhost:8080/RDFTest/api/rdf/Samples?patient.firstName=Donald&patient.lastName=Duck&id=sample1";
    var iris = handler.resources.keySet().stream().map(Objects::toString).toList();
    assertTrue(
        iris.contains(subjectWithCompositeKey),
        "A Sample resource should have a key based on patient.firstName, patient.lastName and id");
  }

  @Test
  void testThatInstancesUseReferToDatasetWithTheRightPredicate() throws IOException {
    var handler = new InMemoryRDFHandler() {};
    getAndParseRDF(Selection.ofRow(petStore_nr1, "Pets", "pooky"), handler);
    for (var iri : handler.resources.keySet()) {
      // Select the triples for pooky
      if (iri.stringValue().endsWith("pooky")) {

        var pooky = handler.resources.get(iri);
        assertTrue(
            pooky.containsKey(RDFService.IRI_DATASET_PREDICATE),
            "An instance of a Pet should refer back to the Collection using qb:dataSet");
        assertFalse(
            pooky.containsKey(RDFService.IRI_DATASET_CLASS), "qb:DataSet is not a predicate");
      }
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
  }
}
