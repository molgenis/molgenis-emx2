package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.json.JsonUtil;

@Disabled
public class BeaconBiosamplesTests extends BeaconModelEndPointTest {

  @Test
  public void testBiosamples_NoParams() throws Exception {
    Context request = mockEntryTypeRequestRegular(EntryType.BIOSAMPLES.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);

    assertTrue(json.contains("\"resultsCount\" : 3,"));
    assertTrue(json.contains("obtentionProcedure"));
    assertTrue(json.contains("procedureCode"));
    assertTrue(json.contains("\"id\" : \"OBI:0002654\""));
    assertTrue(json.contains("\"label\" : \"needle biopsy\""));
  }

  @Test
  public void testBiosamples_NoHits() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(
            EntryType.BIOSAMPLES.getId(), Map.of("id", List.of("Sample0003")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);

    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
  }

  @Test
  public void testBiosamples_IdQuery() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(
            EntryType.BIOSAMPLES.getId(), Map.of("id", List.of("Sample0002")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"id\" : \"Sample0002\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
  }
}
