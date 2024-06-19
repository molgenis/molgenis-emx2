package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import spark.QueryParamsMap;
import spark.Request;

public class BeaconRunsTests extends BeaconModelEndPointTest {

  @Test
  public void testRunsOfIndividual_pathQuery_twoResults() {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    Map<String, String> urlParams =
        Map.of(
            ":entry_type_id", EntryType.INDIVIDUALS.getId(),
            ":entry_type", EntryType.RUNS.getId(),
            ":id", "Ind001");

    when(request.params()).thenReturn(urlParams);
    when(request.queryMap()).thenReturn(mock(QueryParamsMap.class));
    when(request.queryMap().toMap()).thenReturn(new HashMap<>());
    when(request.attribute("specification")).thenReturn("beacon");

    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals(results.get(0).get("individualId").textValue(), "Ind001");
    assertEquals(results.get(1).get("individualId").textValue(), "Ind001");
  }
}
