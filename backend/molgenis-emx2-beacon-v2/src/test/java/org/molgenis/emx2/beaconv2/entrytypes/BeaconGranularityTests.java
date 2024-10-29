package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

public class BeaconGranularityTests extends BeaconModelEndPointTest {

  @Test
  public void testRequestedGranularity_requestBoolean() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
    assertNull(json.get("response").get("resultSets").get(0).get("resultsCount"));
  }

  @Test
  public void testRequestedGranularity_requestCount() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertEquals(5, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }

  @Test
  public void testRequestedGranularity_getRequestCount() {
    Map<String, List<String>> params = Map.of("requestedGranularity", List.of("count"));
    BeaconRequestBody requestBody =
        new BeaconRequestBody(mockEntryTypeRequestRegular("Individuals", params));

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertEquals(5, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }
}
