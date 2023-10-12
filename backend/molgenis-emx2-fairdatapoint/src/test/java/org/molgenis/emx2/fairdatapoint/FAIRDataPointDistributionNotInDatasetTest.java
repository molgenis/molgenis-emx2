package org.molgenis.emx2.fairdatapoint;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

/**
 * FDP Distribution must be referenced as a table name by at least 1 FDP Dataset. If not, this
 * Distribution should not be presented.
 */
@Tag("slow")
public class FAIRDataPointDistributionNotInDatasetTest {

  static Database database;
  static Schema fairDataHub_distribnotindataset;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    fairDataHub_distribnotindataset = database.dropCreateSchema("fairDataHub_distribnotindataset");
    FAIRDataHubLoader fairDataHubLoader = new FAIRDataHubLoader();
    fairDataHubLoader.load(fairDataHub_distribnotindataset, true);
  }

  @Test
  public void FDPBadDistribution() throws Exception {
    // request a distribution that is okay to retrieve
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_distribnotindataset/Analyses/jsonld");
    when(request.params("schema")).thenReturn("fairDataHub_distribnotindataset");
    when(request.params("distribution")).thenReturn("Analyses");
    when(request.params("format")).thenReturn("jsonld");
    FAIRDataPointDistribution fairDataPointDistribution =
        new FAIRDataPointDistribution(request, database);
    String result = fairDataPointDistribution.getResult();
    assertTrue(
        result.contains(
            "dcat:downloadURL <http://localhost:8080/fairDataHub_distribnotindataset/api/jsonld/Analyses>;"));

    // request a distribution that does not exist at all
    request = mock(Request.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_distribnotindataset/something_quite_wrong/jsonld");
    when(request.params("schema")).thenReturn("fairDataHub_distribnotindataset");
    when(request.params("distribution")).thenReturn("something_quite_wrong");
    when(request.params("format")).thenReturn("jsonld");
    Request finalRequest2 = request;
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
