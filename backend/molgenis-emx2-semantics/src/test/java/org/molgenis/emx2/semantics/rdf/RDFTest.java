package org.molgenis.emx2.semantics.rdf;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.semantics.rdf.StringsForRDFTest.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.datamodels.PetStoreLoader;
import org.molgenis.emx2.semantics.RDFService;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;
import spark.Response;

public class RDFTest {

  static Database database;
  static Schema[] petStoreSchemas;
  static final String RDF_API_LOCATION = "/api/rdf";

  @BeforeClass
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
  public void RDFForDatabaseAsTTL() {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.url()).thenReturn("http://localhost:8080/api/fdp");
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, null, null, petStoreSchemas);
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
    assertTrue(result.contains(TTL_COL_CATEGORY_1));
    assertTrue(result.contains(TTL_COL_CATEGORY_2));
    assertTrue(result.contains(TTL_COL_PET_1));
    assertTrue(result.contains(TTL_COL_PET_2));
    assertTrue(result.contains(TTL_ROW_POOKY_1));
    assertTrue(result.contains(TTL_ROW_SPIKE_1));
    assertTrue(result.contains(TTL_ROW_POOKY_2));
    assertTrue(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertTrue(result.contains(TTL_ROW_DOG_1));
    assertTrue(result.contains(TTL_ROW_CAT_2));
    assertTrue(result.contains(TTL_ROW_DOG_2));
    assertEquals(52443, result.length());
  }

  @Test
  public void RDFForOneSchemaAsTTL() {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.url()).thenReturn("http://localhost:8080/petStoreNr1/api/fdp");
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, null, null, petStoreSchemas[0]);
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
    assertTrue(result.contains(TTL_COL_CATEGORY_1));
    assertFalse(result.contains(TTL_COL_CATEGORY_2));
    assertTrue(result.contains(TTL_COL_PET_1));
    assertFalse(result.contains(TTL_COL_PET_2));
    assertTrue(result.contains(TTL_ROW_POOKY_1));
    assertTrue(result.contains(TTL_ROW_SPIKE_1));
    assertFalse(result.contains(TTL_ROW_POOKY_2));
    assertFalse(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertTrue(result.contains(TTL_ROW_DOG_1));
    assertFalse(result.contains(TTL_ROW_CAT_2));
    assertFalse(result.contains(TTL_ROW_DOG_2));
    assertEquals(26495, result.length());
  }

  @Test
  public void RDFForOneTableAsTTL() {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.url()).thenReturn("http://localhost:8080/petStore/api/fdp/Category");
    Table table = petStoreSchemas[0].getTable("Category");
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, table, null, table.getSchema());
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
    assertTrue(result.contains(TTL_COL_CATEGORY_1));
    assertFalse(result.contains(TTL_COL_CATEGORY_2));
    assertFalse(result.contains(TTL_COL_PET_1));
    assertFalse(result.contains(TTL_COL_PET_2));
    assertFalse(result.contains(TTL_ROW_POOKY_1));
    assertFalse(result.contains(TTL_ROW_SPIKE_1));
    assertFalse(result.contains(TTL_ROW_POOKY_2));
    assertFalse(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertTrue(result.contains(TTL_ROW_DOG_1));
    assertFalse(result.contains(TTL_ROW_CAT_2));
    assertFalse(result.contains(TTL_ROW_DOG_2));
    assertEquals(3518, result.length());
  }

  @Test
  public void RDFForOneRowAsTTL() {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.url()).thenReturn("http://localhost:8080/petStore/api/fdp/Category/cat");
    Table table = petStoreSchemas[0].getTable("Category");
    String rowId = "cat";
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, table, rowId, table.getSchema());
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
    assertTrue(result.contains(TTL_COL_CATEGORY_1));
    assertFalse(result.contains(TTL_COL_CATEGORY_2));
    assertFalse(result.contains(TTL_COL_PET_1));
    assertFalse(result.contains(TTL_COL_PET_2));
    assertFalse(result.contains(TTL_ROW_POOKY_1));
    assertFalse(result.contains(TTL_ROW_SPIKE_1));
    assertFalse(result.contains(TTL_ROW_POOKY_2));
    assertFalse(result.contains(TTL_ROW_SPIKE_2));
    assertTrue(result.contains(TTL_ROW_CAT_1));
    assertFalse(result.contains(TTL_ROW_DOG_1));
    assertFalse(result.contains(TTL_ROW_CAT_2));
    assertFalse(result.contains(TTL_ROW_DOG_2));
    assertEquals(2910, result.length());
  }

  @Test
  public void RDFForOneRowAsXML() {
    Request request = mock(Request.class);
    Response response = mock(Response.class);
    when(request.url())
        .thenReturn("http://localhost:8080/petStore/api/fdp/Category/cat?format=xml");
    when(request.queryParams("format")).thenReturn("xml");
    Table table = petStoreSchemas[0].getTable("Category");
    String rowId = "cat";
    OutputStream outputStream = new ByteArrayOutputStream();
    RDFService.describeAsRDF(
        outputStream, request, response, RDF_API_LOCATION, table, rowId, table.getSchema());
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
    assertEquals(6285, result.length());
  }
}
