package org.molgenis.emx2.datamodels.beacon.entrytypes;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.PatientRegistryTest;
import org.molgenis.emx2.datamodels.beacon.BeaconTestUtil;

public class BeaconAnalysisTest extends PatientRegistryTest {

  @Test
  public void testAnalyses_NoParams() {
    Context request = BeaconTestUtil.mockEntryTypeRequestRegular("analyses", new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    assertEquals(4, analyses.get("responseSummary").get("numTotalResults").intValue());
  }

  @Test
  public void testAnalyses_NoHits() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            EntryType.ANALYSES.getName(), Map.of("id", List.of("A05")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    assertFalse(analyses.get("responseSummary").get("exists").booleanValue());
  }

  @Test
  public void testAnalyses_IdQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            EntryType.ANALYSES.getName(), Map.of("id", List.of("A03")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    assertTrue(analyses.get("responseSummary").get("exists").booleanValue());
    assertEquals(1, analyses.get("responseSummary").get("numTotalResults").intValue());
  }
}
