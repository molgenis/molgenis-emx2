package org.molgenis.emx2.semantics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.datamodels.FAIRDataHubLoader.createSchema;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.fairdatapoint.FAIRDataPoint;
import org.molgenis.emx2.io.MolgenisIO;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

public class FAIRDataPointNoCatalogsTest {

  static Database database;
  static Schema fairDataHub_nocatalogs;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    fairDataHub_nocatalogs = database.dropCreateSchema("fairDataHub_nocatalogs");
    createSchema(fairDataHub_nocatalogs, "fairdatahub/fairdatapoint/molgenis.csv");
    MolgenisIO.fromClasspathDirectory("fairdatahub/ontologies", fairDataHub_nocatalogs, false);
    MolgenisIO.fromClasspathDirectory(
        "fairdatahub/fairdatapoint/demodata", fairDataHub_nocatalogs, false);
    fairDataHub_nocatalogs.dropTable("FDP_Catalog");
  }

  @Test
  public void FDPNoDataset() throws Exception {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/fdp");
    FAIRDataPoint fairDataPoint = new FAIRDataPoint(request, fairDataHub_nocatalogs);
    String result = fairDataPoint.getResult();
    assertFalse(result.contains("fdp-o:metadataCatalog"));
    assertFalse(result.contains("ldp:DirectContainer"));
    assertFalse(result.contains("dcterms:title \"Catalogs\";"));
    assertFalse(result.contains("ldp:hasMemberRelation"));
    assertFalse(result.contains("ldp:membershipResource"));
    assertFalse(result.contains("ldp:contains"));
    assertEquals(2788, result.length());
  }
}
