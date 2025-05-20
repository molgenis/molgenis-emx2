package org.molgenis.emx2.datamodels.beacon.entrytypes;

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
import org.molgenis.emx2.datamodels.PatientRegistryTest;
import org.molgenis.emx2.datamodels.beacon.BeaconTestUtil;

@Disabled
public class BeaconAnalysisTest extends PatientRegistryTest {

  @Test
  public void testAnalyses_NoParams() {
    Context request = BeaconTestUtil.mockEntryTypeRequestRegular("analyses", new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    analyses.get(0);
    ////    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    //    assertTrue(json.contains("\"resultsCount\" : 5,"));
  }

  @Test
  public void testAnalyses_NoHits() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            EntryType.ANALYSES.getId(), Map.of("id", List.of("A05")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    ////    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    //    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
  }

  @Test
  public void testAnalyses_IdQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            EntryType.ANALYSES.getId(), Map.of("id", List.of("A03")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    //    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    //    assertTrue(json.contains("\"id\" : \"A03\","));
    //    assertTrue(json.contains("\"resultsCount\" : 1,"));
  }
}
