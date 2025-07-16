package org.molgenis.emx2.datamodels.beacon.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.molgenis.emx2.datamodels.beacon.BeaconTestUtil.mockIndividualsPostRequestRegular;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.TestLoaders;
import org.molgenis.emx2.datamodels.beacon.BeaconTestUtil;
import org.molgenis.emx2.graphql.GraphqlSession;

public class BeaconGranularityTests extends TestLoaders {

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
    JsonNode json = queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
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
    JsonNode json = queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertEquals(23, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }

  @Test
  public void testRequestedGranularity_getRequestCount() {
    Map<String, List<String>> params = Map.of("requestedGranularity", List.of("count"));
    BeaconRequestBody requestBody =
        new BeaconRequestBody(BeaconTestUtil.mockEntryTypeRequestRegular("Individuals", params));

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertEquals(23, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }
}
