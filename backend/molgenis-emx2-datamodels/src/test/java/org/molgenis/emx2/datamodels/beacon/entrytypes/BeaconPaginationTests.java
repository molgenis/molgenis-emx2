package org.molgenis.emx2.datamodels.beacon.entrytypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.datamodels.beacon.BeaconTestUtil.mockIndividualsPostRequestRegular;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.TestLoaders;

public class BeaconPaginationTests extends TestLoaders {

  @Test
  public void testPagination_TwoItems_Offset0() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "pagination": {
                                "limit": 2,
                                "skip": 0
                              }
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(patientRegistry);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals("Case1F", results.get(0).get("id").textValue());
    assertEquals("Case1C", results.get(1).get("id").textValue());
  }

  @Test
  public void testPagination_TwoItems_Offset2() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "pagination": {
                                "limit": 2,
                                "skip": 2
                              }
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(patientRegistry);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals("Case1M", results.get(0).get("id").textValue());
    assertEquals("Case2C", results.get(1).get("id").textValue());
  }

  @Test
  public void testPagination_LimitZero_AllResult() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "pagination": {
                                "limit": 0,
                                "skip": 0
                              }
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(patientRegistry);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(23, results.size());
  }
}
