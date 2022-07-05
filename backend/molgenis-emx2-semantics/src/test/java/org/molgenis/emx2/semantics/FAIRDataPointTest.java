package org.molgenis.emx2.semantics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.fairdatapoint.FAIRDataPoint;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

public class FAIRDataPointTest {

  static Database database;
  static List<Schema> fairDataHubSchemas;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    Schema fairDataHub_nr1 = database.dropCreateSchema("fairDataHub_nr1");
    Schema fairDataHub_nr2 = database.dropCreateSchema("fairDataHub_nr2");
    FAIRDataHubLoader b2l = new FAIRDataHubLoader();
    b2l.load(fairDataHub_nr1, true);
    b2l.load(fairDataHub_nr2, true);
    fairDataHubSchemas = new ArrayList<>();
    fairDataHubSchemas.add(fairDataHub_nr1);
    fairDataHubSchemas.add(fairDataHub_nr2);
  }

  @Test
  public void FDPMetadataSchemaService() throws Exception {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/fdp");
    FAIRDataPoint fairDataPoint = new FAIRDataPoint(request, fairDataHubSchemas);
    String result = fairDataPoint.getResult();
    System.out.println(result);
    assertTrue(
        result.contains(
            """
            <http://localhost:8080/api/fdp> a fdp-o:MetadataService, dcat:Resource, dcat:DataService,
                fdp-o:FAIRDataPoint;
              dcterms:title "FAIR Data Point hosted by MOLGENIS-EMX2 at http://localhost:8080/api/fdp";
              dcterms:publisher [ a foaf:Agent;
                  foaf:name "MOLGENIS-EMX2 FAIR Data Point API"
                ];"""));
    assertTrue(
        result.contains(
            """
            ldp:contains <http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId01>,
              <http://localhost:8080/api/fdp/catalog/fairDataHub_nr1/catalogId02>, <http://localhost:8080/api/fdp/catalog/fairDataHub_nr2/catalogId01>,
              <http://localhost:8080/api/fdp/catalog/fairDataHub_nr2/catalogId02> ."""
                .indent(2)));
    assertEquals(3751, result.length());
  }
}
