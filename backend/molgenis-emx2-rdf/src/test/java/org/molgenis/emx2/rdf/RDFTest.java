package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RDFTest {

  static Database database;
  static List<Schema> petStoreSchemas;
  static final String RDF_API_LOCATION = "/api/rdf";
  static Schema petStore_nr1;
  static Schema petStore_nr2;

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
  }

  @AfterAll
  public static void tearDown() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema petStore_nr1 = database.dropCreateSchema("petStoreNr1");
    Schema petStore_nr2 = database.dropCreateSchema("petStoreNr2");
  }

  @Test
  void testThatColumnsAreAProperty() throws IOException {
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, null, null, null, petStore_nr1);
    String result = outputStream.toString();
    var parser = Rio.createParser(RDFFormat.TURTLE);
    parser.setRDFHandler(
        new InMemoryRDFHandler() {
          @Override
          public void handleStatement(Statement st) throws RDFHandlerException {
            super.handleStatement(st);
            if (st.getPredicate().equals(RDF.TYPE)) {
              if (st.getSubject().stringValue().contains("/column/")) {
                assertTrue(
                    st.getObject().equals(OWL.OBJECTPROPERTY)
                        || st.getObject().equals(OWL.DATATYPEPROPERTY)
                        || st.getObject().equals(OWL.ANNOTATEDPROPERTY),
                    "Columns must be defined as rdf:type one of owl:objectProperty, owl:dataProperty or owl:annotationProperty");
              }
            }
          }
        });
    parser.parse(new StringReader(result));
  }

  @Test
  void testThatTablesAreClasses() throws IOException {
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, null, null, null, petStore_nr1);
    String result = outputStream.toString();
    var parser = Rio.createParser(RDFFormat.TURTLE);
    var handler = new InMemoryRDFHandler() {};
    parser.setRDFHandler(handler);
    parser.parse(new StringReader(result));
    for (var resource : handler.resources.entrySet()) {
      var subClasses = resource.getValue().get(RDFS.SUBCLASSOF);
      if (subClasses != null && subClasses.contains(RDFService.IRI_DATABASE_TABLE)) {
        var types = resource.getValue().get(RDF.TYPE);
        var subject = resource.getKey().stringValue();
        System.err.println("==> " + subject + " types " + types);
        assertNotNull(types, subject + " should have a rdf:Type.");
        assertTrue(types.contains(OWL.CLASS), subject + " should be a owl:Class.");
      }
    }
  }

  @Test
  void testThatClassesDoNotHaveRangeOrDomain() throws IOException {
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, null, null, null, petStore_nr1);
    String result = outputStream.toString();
    var parser = Rio.createParser(RDFFormat.TURTLE);
    var handler = new InMemoryRDFHandler() {};
    parser.setRDFHandler(handler);
    parser.parse(new StringReader(result));
    for (var resource : handler.resources.entrySet()) {
      var types = resource.getValue().get(RDF.TYPE);
      if (types != null && types.contains(OWL.CLASS)) {
        var subject = resource.getKey().stringValue();
        assertFalse(
            resource.getValue().containsKey(RDFS.DOMAIN),
            subject + " can't have a rdfs:Domain, since it is a class.");
        assertFalse(
            resource.getValue().containsKey(RDFS.RANGE),
            subject + "can't have a rdfs:Range, since it is a class.");
      }
    }
  }

  @Test
  void testThatRDFOnlyIncludesRequestedSchema() throws IOException {
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, null, null, null, petStore_nr1);
    String result = outputStream.toString();
    var parser = Rio.createParser(RDFFormat.TURTLE);
    var handler = new InMemoryRDFHandler() {};
    parser.setRDFHandler(handler);
    parser.parse(new StringReader(result));
    for (var resource : handler.resources.keySet()) {
      assertFalse(resource.toString().contains("petStoreNr2"));
    }
  }
}
