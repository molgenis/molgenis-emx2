package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

public class BeaconGranularityTests extends BeaconModelEndPointTest {

  @Test
  public void testRequestedGranularity_requestBoolean() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
    assertNull(json.get("response").get("resultSets").get(0).get("resultsCount"));
  }

  @Test
  public void testRequestedGranularity_requestCount() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    assertEquals(5, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }
}
