package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.json.JsonUtil;
import spark.Request;

public class BeaconCohortsTests extends BeaconModelEndPointTest {

  @Test
  public void testCohorts_NoParams() throws Exception {
    Request request = mockEntryTypeRequestRegular(EntryType.COHORTS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode cohorts = queryEntryType.query(database);

    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    assertTrue(json.contains("\"id\" : \"Cohort0001\""));
    assertTrue(json.contains("\"id\" : \"ISO3166:FR\""));
  }

  @Test
  public void testCohorts_NoHits() throws Exception {
    Request request =
        mockEntryTypeRequestRegular(
            EntryType.COHORTS.getId(), Map.of("id", new String[] {"Cohort0003"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode cohorts = queryEntryType.query(database);

    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    assertTrue(json.contains("\"collections\" : [ ]"));
  }
}
