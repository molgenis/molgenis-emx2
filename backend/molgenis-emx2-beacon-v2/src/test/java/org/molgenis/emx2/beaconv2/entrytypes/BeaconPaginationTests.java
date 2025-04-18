package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

@Disabled
public class BeaconPaginationTests extends BeaconModelEndPointTest {

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
    JsonNode json = queryEntryType.query(beaconSchema);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals("Ind001", results.get(0).get("id").textValue());
    assertEquals("Ind002", results.get(1).get("id").textValue());
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
    JsonNode json = queryEntryType.query(beaconSchema);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals("MinIndNoRefs003", results.get(0).get("id").textValue());
    assertEquals("MinInd004", results.get(1).get("id").textValue());
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
    JsonNode json = queryEntryType.query(beaconSchema);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(5, results.size());
  }
}
