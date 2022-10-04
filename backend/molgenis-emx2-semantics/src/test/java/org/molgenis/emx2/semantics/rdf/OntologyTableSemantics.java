package org.molgenis.emx2.semantics.rdf;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SchemaMetadata;
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.io.emx2.Emx2;
import org.molgenis.emx2.io.readers.CsvTableReader;
import org.molgenis.emx2.semantics.RDFService;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;
import spark.Response;

public class OntologyTableSemantics {

  static Database database;
  static Schema petStoreSchema;
  static final String RDF_API_LOCATION = "/api/rdf";

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema petStore = database.dropCreateSchema("petStore");
    PetStoreLoader petStoreLoader = new PetStoreLoader();
    petStoreLoader.load(petStore, true);
    petStoreSchema = petStore;
  }

  @Test
  public void OntologyTableSemantics() {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.url()).thenReturn("http://localhost:8080/petStore/api/fdp");
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, null, null, petStoreSchema);
    String result = outputStream.toString();

    /**
     * Situation before: the 'Tag' ontology table has the default annotation of NCIT:C48697
     * (Controlled Vocabulary)
     */
    assertTrue(
        result.contains(
            "emx0:Tag a owl:Class, qb:DataSet, sio:SIO_000754;\n"
                + "  rdfs:isDefinedBy <http://purl.obolibrary.org/obo/NCIT_C48697>;"));
    assertFalse(
        result.contains(
            "emx0:Tag a owl:Class, qb:DataSet, sio:SIO_000754;\n"
                + "  rdfs:isDefinedBy <https://w3id.org/reproduceme#Tag>;"));

    /** Update the 'Tag' ontology table with new semantics and produce new RDF */
    SchemaMetadata metadata =
        Emx2.fromRowList(
            CsvTableReader.read(
                new InputStreamReader(
                    FAIRDataHubLoader.class
                        .getClassLoader()
                        .getResourceAsStream("OntologyTableSemanticsTestFile.csv"))));
    petStoreSchema.migrate(metadata);

    outputStream = new ByteArrayOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, null, null, petStoreSchema);
    result = outputStream.toString();

    /**
     * Situation after: the 'Tag' ontology table has the Tag annotation from the REPRODUCE-ME
     * ontology
     */
    assertFalse(
        result.contains(
            "emx0:Tag a owl:Class, qb:DataSet, sio:SIO_000754;\n"
                + "  rdfs:isDefinedBy <http://purl.obolibrary.org/obo/NCIT_C48697>;"));
    assertTrue(
        result.contains(
            "emx0:Tag a owl:Class, qb:DataSet, sio:SIO_000754;\n"
                + "  rdfs:isDefinedBy <https://w3id.org/reproduceme#Tag>;"));
  }
}
