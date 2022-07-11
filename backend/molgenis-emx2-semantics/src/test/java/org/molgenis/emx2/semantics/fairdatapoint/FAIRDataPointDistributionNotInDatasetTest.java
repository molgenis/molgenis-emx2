package org.molgenis.emx2.semantics.fairdatapoint;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.fairdatapoint.FAIRDataPointDistribution;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

/**
 * FDP Distribution must be referenced as a table name by at least 1 FDP Dataset. If not, this
 * Distribution should not be presented.
 */
public class FAIRDataPointDistributionNotInDatasetTest {

  static Database database;
  static Schema fairDataHub_distribnotindataset;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    fairDataHub_distribnotindataset = database.dropCreateSchema("fairDataHub_distribnotindataset");
    FAIRDataHubLoader fairDataHubLoader = new FAIRDataHubLoader();
    fairDataHubLoader.load(fairDataHub_distribnotindataset, true);
  }

  @Test
  public void FDPBadDistribution() throws Exception {
    // request a distribution that is part of a dataset and thus is okay to retrieve
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_distribnotindataset/Analyses/jsonld");
    when(request.params("schema")).thenReturn("fairDataHub_distribnotindataset");
    when(request.params("table")).thenReturn("Analyses");
    when(request.params("format")).thenReturn("jsonld");
    FAIRDataPointDistribution fairDataPointDistribution =
        new FAIRDataPointDistribution(request, database);
    String result = fairDataPointDistribution.getResult();
    assertTrue(
        result.contains(
            "dcat:downloadURL <http://localhost:8080/fairDataHub_distribnotindataset/api/jsonld/Analyses>;"));

    // request a distribution that exists but is NOT part of a dataset and should not be retrievable
    request = mock(Request.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_distribnotindataset/Runs/jsonld");
    when(request.params("schema")).thenReturn("fairDataHub_distribnotindataset");
    when(request.params("table")).thenReturn("Runs");
    when(request.params("format")).thenReturn("jsonld");
    Request finalRequest = request;
    Exception exception =
        assertThrows(
            Exception.class,
            () -> {
              new FAIRDataPointDistribution(finalRequest, database);
            });
    String expectedMessage =
        "Requested table distribution exists within schema, but is not part of any dataset in the schema and is therefore not retrievable.";
    String actualMessage = exception.getMessage();
    assertTrue(actualMessage.contains(expectedMessage));

    // request a distribution that does not exist at all
    request = mock(Request.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/distribution/fairDataHub_distribnotindataset/something_quite_wrong/jsonld");
    when(request.params("schema")).thenReturn("fairDataHub_distribnotindataset");
    when(request.params("table")).thenReturn("something_quite_wrong");
    when(request.params("format")).thenReturn("jsonld");
    Request finalRequest2 = request;
    Exception exception2 =
        assertThrows(
            Exception.class,
            () -> {
              new FAIRDataPointDistribution(finalRequest2, database);
            });
    String expectedMessage2 = "Table unknown.";
    String actualMessage2 = exception2.getMessage();
    assertTrue(actualMessage2.contains(expectedMessage2));
  }
}
