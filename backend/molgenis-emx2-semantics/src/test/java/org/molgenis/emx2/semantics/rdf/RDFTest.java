package org.molgenis.emx2.semantics.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.semantics.rdf.StringsForRDFTest.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.semantics.RDFService;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class RDFTest {

  static Database database;
  static Schema[] petStoreSchemas;
  static final String RDF_API_LOCATION = "/api/rdf";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema petStore_nr1 = database.dropCreateSchema("petStoreNr1");
    Schema petStore_nr2 = database.dropCreateSchema("petStoreNr2");
    PetStoreLoader petStoreLoader = new PetStoreLoader();
    petStoreLoader.load(petStore_nr1, true);
    petStoreLoader.load(petStore_nr2, true);
    petStoreSchemas = new Schema[2];
    petStoreSchemas[0] = petStore_nr1;
    petStoreSchemas[1] = petStore_nr2;
  }

  @Test
  void RDFForDatabaseAsTTL() {
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService rdf = new RDFService("http://localhost:8080" + RDF_API_LOCATION);
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, null, null, null, petStoreSchemas);
    String result = outputStream.toString();
    assertTrue(result.contains(TTL_PREFIX_1));
    assertTrue(result.contains(TTL_PREFIX_2));
    assertTrue(result.contains(TTL_ROOT));
    assertTrue(result.contains(TTL_SCHEMA_1));
    assertTrue(result.contains(TTL_SCHEMA_2));
    assertTrue(result.contains(TTL_TABLE_CATEGORY_1));
    assertTrue(result.contains(TTL_TABLE_CATEGORY_2));
    assertTrue(result.contains(TTL_TABLE_PET_1));
    assertTrue(result.contains(TTL_TABLE_PET_2));
    assertTrue(result.contains(TTL_COL_CATEGORY_NAME_1));
    assertTrue(result.contains(TTL_COL_CATEGORY_NAME_2));
    assertTrue(result.contains(TTL_COL_PET_NAME_1));
    assertTrue(result.contains(TTL_COL_PET_NAME_2));
    assertTrue(result.contains(TTL_COL_PET_DETAILS_1));
    assertTrue(result.contains(TTL_COL_PET_DETAILS_2));
    assertTrue(result.contains(TTL_ROW_POOKY_1));
    assertTrue(result.contains(TTL_ROW_SPIKE_1));
    assertTrue(result.contains(TTL_ROW_POOKY_2));
    assertTrue(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertTrue(result.contains(TTL_ROW_DOG_1));
    assertTrue(result.contains(TTL_ROW_CAT_2));
    assertTrue(result.contains(TTL_ROW_DOG_2));
  }

  @Test
  void RDFForOneSchemaAsTTL() {
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService rdf = new RDFService("http://localhost:8080/petStoreNr1" + RDF_API_LOCATION);
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, null, null, null, petStoreSchemas[0]);
    String result = outputStream.toString();
    assertTrue(result.contains(TTL_PREFIX_1));
    assertFalse(result.contains(TTL_PREFIX_2));
    assertTrue(result.contains(TTL_ROOT));
    assertTrue(result.contains(TTL_SCHEMA_1));
    assertFalse(result.contains(TTL_SCHEMA_2));
    assertTrue(result.contains(TTL_TABLE_CATEGORY_1));
    assertFalse(result.contains(TTL_TABLE_CATEGORY_2));
    assertTrue(result.contains(TTL_TABLE_PET_1));
    assertFalse(result.contains(TTL_TABLE_PET_2));
    assertTrue(result.contains(TTL_COL_CATEGORY_NAME_1));
    assertFalse(result.contains(TTL_COL_CATEGORY_NAME_2));
    assertTrue(result.contains(TTL_COL_PET_NAME_1));
    assertFalse(result.contains(TTL_COL_PET_NAME_2));
    assertTrue(result.contains(TTL_COL_PET_DETAILS_1));
    assertFalse(result.contains(TTL_COL_PET_DETAILS_2));
    assertTrue(result.contains(TTL_ROW_POOKY_1));
    assertTrue(result.contains(TTL_ROW_SPIKE_1));
    assertFalse(result.contains(TTL_ROW_POOKY_2));
    assertFalse(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertTrue(result.contains(TTL_ROW_DOG_1));
    assertFalse(result.contains(TTL_ROW_CAT_2));
    assertFalse(result.contains(TTL_ROW_DOG_2));
  }

  @Test
  void RDFForOneTableAsTTL() {
    RDFService rdf =
        new RDFService("http://localhost:8080/petStore" + RDF_API_LOCATION + "/Category");
    Table table = petStoreSchemas[0].getTable("Category");
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, table, null, null, table.getSchema());
    String result = outputStream.toString();
    assertTrue(result.contains(TTL_PREFIX_1));
    assertFalse(result.contains(TTL_PREFIX_2));
    assertTrue(result.contains(TTL_ROOT));
    assertTrue(result.contains(TTL_SCHEMA_1));
    assertFalse(result.contains(TTL_SCHEMA_2));
    assertTrue(result.contains(TTL_TABLE_CATEGORY_1));
    assertFalse(result.contains(TTL_TABLE_CATEGORY_2));
    assertFalse(result.contains(TTL_TABLE_PET_1));
    assertFalse(result.contains(TTL_TABLE_PET_2));
    assertTrue(result.contains(TTL_COL_CATEGORY_NAME_1));
    assertFalse(result.contains(TTL_COL_CATEGORY_NAME_2));
    assertFalse(result.contains(TTL_COL_PET_NAME_1));
    assertFalse(result.contains(TTL_COL_PET_NAME_2));
    assertFalse(result.contains(TTL_COL_PET_DETAILS_1));
    assertFalse(result.contains(TTL_COL_PET_DETAILS_2));
    assertFalse(result.contains(TTL_ROW_POOKY_1));
    assertFalse(result.contains(TTL_ROW_SPIKE_1));
    assertFalse(result.contains(TTL_ROW_POOKY_2));
    assertFalse(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertTrue(result.contains(TTL_ROW_DOG_1));
    assertFalse(result.contains(TTL_ROW_CAT_2));
    assertFalse(result.contains(TTL_ROW_DOG_2));
  }

  @Test
  void RDFForOneColumnAsTTL() {
    Table table = petStoreSchemas[0].getTable("Pet");
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService rdf =
        new RDFService("http://localhost:8080/petStore" + RDF_API_LOCATION + "/Pet/column/details");
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, table, null, "details", table.getSchema());
    String result = outputStream.toString();
    assertTrue(result.contains(TTL_PREFIX_1));
    assertFalse(result.contains(TTL_PREFIX_2));
    assertTrue(result.contains(TTL_ROOT));
    assertTrue(result.contains(TTL_SCHEMA_1));
    assertFalse(result.contains(TTL_SCHEMA_2));
    assertFalse(result.contains(TTL_TABLE_CATEGORY_1));
    assertFalse(result.contains(TTL_TABLE_CATEGORY_2));
    assertTrue(result.contains(TTL_TABLE_PET_1));
    assertFalse(result.contains(TTL_TABLE_PET_2));
    assertFalse(result.contains(TTL_COL_CATEGORY_NAME_1));
    assertFalse(result.contains(TTL_COL_CATEGORY_NAME_2));
    assertFalse(result.contains(TTL_COL_PET_NAME_1));
    assertFalse(result.contains(TTL_COL_PET_NAME_2));
    assertTrue(result.contains(TTL_COL_PET_DETAILS_1));
    assertFalse(result.contains(TTL_COL_PET_DETAILS_2));
    assertFalse(result.contains(TTL_ROW_POOKY_1));
    assertFalse(result.contains(TTL_ROW_SPIKE_1));
    assertFalse(result.contains(TTL_ROW_POOKY_2));
    assertFalse(result.contains(TTL_ROW_SPIKE_2));
    assertFalse(result.contains(TTL_ROW_CAT_1));
    assertFalse(result.contains(TTL_ROW_DOG_1));
    assertFalse(result.contains(TTL_ROW_CAT_2));
    assertFalse(result.contains(TTL_ROW_DOG_2));
  }

  @Test
  void RDFForOneRowAsTTL() {
    RDFService rdf =
        new RDFService("http://localhost:8080/petStore" + RDF_API_LOCATION + "/Category/cat");
    Table table = petStoreSchemas[0].getTable("Category");
    String rowId = "cat";
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, table, rowId, null, table.getSchema());
    String result = outputStream.toString();
    assertTrue(result.contains(TTL_PREFIX_1));
    assertFalse(result.contains(TTL_PREFIX_2));
    assertTrue(result.contains(TTL_ROOT));
    assertTrue(result.contains(TTL_SCHEMA_1));
    assertFalse(result.contains(TTL_SCHEMA_2));
    assertTrue(result.contains(TTL_TABLE_CATEGORY_1));
    assertFalse(result.contains(TTL_TABLE_CATEGORY_2));
    assertFalse(result.contains(TTL_TABLE_PET_1));
    assertFalse(result.contains(TTL_TABLE_PET_2));
    assertTrue(result.contains(TTL_COL_CATEGORY_NAME_1));
    assertFalse(result.contains(TTL_COL_CATEGORY_NAME_2));
    assertFalse(result.contains(TTL_COL_PET_NAME_1));
    assertFalse(result.contains(TTL_COL_PET_NAME_2));
    assertFalse(result.contains(TTL_COL_PET_DETAILS_1));
    assertFalse(result.contains(TTL_COL_PET_DETAILS_2));
    assertFalse(result.contains(TTL_ROW_POOKY_1));
    assertFalse(result.contains(TTL_ROW_SPIKE_1));
    assertFalse(result.contains(TTL_ROW_POOKY_2));
    assertFalse(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertFalse(result.contains(TTL_ROW_DOG_1));
    assertFalse(result.contains(TTL_ROW_CAT_2));
    assertFalse(result.contains(TTL_ROW_DOG_2));
  }

  @Test
  void RDFForOneRowAsXML() {
    RDFService rdf =
        new RDFService(
            "http://localhost:8080/petStore" + RDF_API_LOCATION + "/Category/cat?format=xml",
            "xml");
    Table table = petStoreSchemas[0].getTable("Category");
    String rowId = "cat";
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, table, rowId, null, table.getSchema());
    String result = outputStream.toString();
    assertTrue(result.contains("xmlns:emx0=\"http://localhost:8080/petStoreNr1/api/rdf/\">"));
    assertTrue(result.contains("<rdf:Description rdf:about=\"http://localhost:8080\">"));
    assertTrue(
        result.contains(
            "<rdf:Description rdf:about=\"http://localhost:8080/petStoreNr1/api/rdf\">"));
    assertTrue(
        result.contains(
            "<rdf:Description rdf:about=\"http://localhost:8080/petStoreNr1/api/rdf/Category\">"));
    assertTrue(
        result.contains(
            "<rdf:Description rdf:about=\"http://localhost:8080/petStoreNr1/api/rdf/Category/column/name\">"));
  }

  @Test
  void RDFUpdateOntologySemantics() {
    RDFService rdf = new RDFService("http://localhost:8080/petStore" + RDF_API_LOCATION + "/Tag");
    Table table = petStoreSchemas[0].getTable("Tag");
    OutputStream outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, table, null, null, table.getSchema());
    String result = outputStream.toString();

    // expect the default tag ("Controlled Vocabulary") but not NCIT_C25586 ("New")
    assertTrue(result.contains("rdfs:isDefinedBy <http://purl.obolibrary.org/obo/NCIT_C48697>"));
    assertFalse(result.contains("rdfs:isDefinedBy <http://purl.obolibrary.org/obo/NCIT_C25586>"));

    // update table semantics and re-create RDF output
    table.getMetadata().setSemantics("http://purl.obolibrary.org/obo/NCIT_C25586");
    System.out.println("getSem = " + table.getMetadata().getSemantics()[0]);
    outputStream = new ByteArrayOutputStream();
    rdf.describeAsRDF(outputStream, RDF_API_LOCATION, table, null, null, table.getSchema());
    result = outputStream.toString();

    // expect NCIT_C25586 ("New") and no longer the default tag ("Controlled Vocabulary")
    assertFalse(result.contains("rdfs:isDefinedBy <http://purl.obolibrary.org/obo/NCIT_C48697>"));
    assertTrue(result.contains("rdfs:isDefinedBy <http://purl.obolibrary.org/obo/NCIT_C25586>"));
  }
}
