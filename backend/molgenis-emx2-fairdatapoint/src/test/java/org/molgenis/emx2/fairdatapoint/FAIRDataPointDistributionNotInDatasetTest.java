package org.molgenis.emx2.fairdatapoint;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.datamodels.DataModels.Profile.DCAT;

import io.javalin.http.Context;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/**
 * FDP Distribution must be referenced as a table name by at least 1 FDP Dataset. If not, this
 * Distribution should not be presented.
 */
@Tag("slow")
public class FAIRDataPointDistributionNotInDatasetTest {

  static Database database;
  static Schema dcat_distribnotindataset;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    dcat_distribnotindataset = database.dropCreateSchema("dcat_distribnotindataset");
    DCAT.getImportTask(dcat_distribnotindataset, true).run();
  }

  @Test
  @Disabled
  public void FDPBadDistribution() throws Exception {
    // request a distribution that is okay to retrieve
    Context request = mock(Context.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_distribnotindataset/Analyses/jsonld");
    when(request.pathParam("schema")).thenReturn("fairDataHub_distribnotindataset");
    when(request.pathParam("distribution")).thenReturn("Analyses");
    when(request.pathParam("format")).thenReturn("jsonld");
    FAIRDataPointDistribution fairDataPointDistribution =
        new FAIRDataPointDistribution(request, database);
    String result = fairDataPointDistribution.getResult();
    assertTrue(
        result.contains(
            "dcat:downloadURL <http://localhost:8080/fairDataHub_distribnotindataset/api/jsonld/Analyses>;"));

    // request a distribution that does not exist at all
    request = mock(Context.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_distribnotindataset/something_quite_wrong/jsonld");
    when(request.pathParam("schema")).thenReturn("fairDataHub_distribnotindataset");
    when(request.pathParam("distribution")).thenReturn("something_quite_wrong");
    when(request.pathParam("format")).thenReturn("jsonld");
    Context finalRequest2 = request;
    Exception exception2 =
        assertThrows(
            Exception.class,
            () -> {
              new FAIRDataPointDistribution(finalRequest2, database);
            });
    String expectedMessage2 = "Distribution or file therein not found";
    String actualMessage2 = exception2.getMessage();
    assertTrue(actualMessage2.contains(expectedMessage2));
  }
}
