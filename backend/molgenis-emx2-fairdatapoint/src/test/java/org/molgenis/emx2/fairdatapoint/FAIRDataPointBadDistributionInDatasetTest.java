package org.molgenis.emx2.fairdatapoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.FAIRDataHubLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.Request;

/**
 * FDP Dataset must have 1+ distributions, in EMX2 represented by 1 table name. The FDP
 * Distributions endpoint 'generates' distributions by combining table name with different formats.
 * Because FDP Dataset 'distribution' is a table name, it is String datatype, and may not match an
 * actual table. This must be checked at the FDP Dataset endpoint. Technically it could also
 * invalidate the overarching FDP Catalog, which must have 1+ datasets. But since this is a 'dataset
 * problem', we leave it at that level.
 */
@Tag("slow")
public class FAIRDataPointBadDistributionInDatasetTest {

  static Database database;
  static Schema fairDataHub_baddistribution;

  @BeforeAll
  @Disabled
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    fairDataHub_baddistribution = database.dropCreateSchema("fairDataHub_baddistribution");
    FAIRDataHubLoader fairDataHubLoader = new FAIRDataHubLoader();
    fairDataHubLoader.load(fairDataHub_baddistribution, true);
  }

  @Test
  @Disabled
  public void FDPBadDistribution() throws Exception {

    // check correct situation: distribution value matches a table, API returns as normal
    Request request = mock(Request.class);
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/dataset/fairDataHub_baddistribution/datasetId01");
    when(request.params("id")).thenReturn("datasetId01");
    FAIRDataPointDataset fairDataPointDataset =
        new FAIRDataPointDataset(request, fairDataHub_baddistribution.getTable("Dataset"));
    String result = fairDataPointDataset.getResult();
    assertTrue(
        result.contains(
            "dcat:distribution <http://localhost:8080/api/fdp/distribution/fairDataHub_baddistribution/Analyses/csv>,"));

    // set distribution to a value that does NOT corresepond to a table
    Table distribution = fairDataHub_baddistribution.getTable("Distribution");
    Row newRow = new Row();
    Map newBadDistr = new HashMap<>();
    newBadDistr.put("name", "something_quite_wrong");
    newBadDistr.put("type", "Table");
    newBadDistr.put("belongsToDataset", "datasetId01");
    newRow.set(newBadDistr);
    distribution.save(newRow);

    // API should check, find that distribution value does not match a table, and throw error
    when(request.url())
        .thenReturn(
            "http://localhost:8080/api/fdp/dataset/fairDataHub_baddistribution/datasetId01");
    when(request.params("id")).thenReturn("datasetId01");
    Exception exception =
        assertThrows(
            Exception.class,
            () ->
                new FAIRDataPointDataset(request, fairDataHub_baddistribution.getTable("Dataset"))
                    .getResult());
    String expectedMessage =
        "Schema does not contain the requested table for distribution. Make sure the value of 'distribution' in your Dataset matches a table name (from the same schema) you want to publish.";
    String actualMessage = exception.getMessage();
    assertTrue(actualMessage.contains(expectedMessage));
  }
}
