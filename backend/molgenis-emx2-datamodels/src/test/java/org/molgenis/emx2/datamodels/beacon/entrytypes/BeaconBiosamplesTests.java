package org.molgenis.emx2.datamodels.beacon.entrytypes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.datamodels.TestLoaders;

@Disabled(
    "This endpoints is currently not supported because the RD3 model does not contain the attributes")
public class BeaconBiosamplesTests extends TestLoaders {

  @Test
  public void testBiosamples_NoParams() {
    //    //     Context request = mockEntryTypeRequestRegular(EntryType.BIOSAMPLES.getName(), new
    //    // HashMap<>());
    //    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    //
    //    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    //    JsonNode biosamples = queryEntryType.query(patientRegistry);
    //    assertEquals(20, biosamples.get("responseSummary").get("numTotalResults").intValue());
  }

  @Test
  public void testBiosamples_NoHits() {
    //     Context request =
    //         mockEntryTypeRequestRegular(
    //             EntryType.BIOSAMPLES.getName(), Map.of("id", List.of("Sample0003")));
    //    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    //
    //    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    //    JsonNode biosamples = queryEntryType.query(patientRegistry);
    //    assertEquals(0, biosamples.get("responseSummary").get("numTotalResults").intValue());
    //    assertFalse(biosamples.get("responseSummary").get("exists").booleanValue());
  }

  @Test
  public void testBiosamples_IdQuery() {
    //     Context request =
    //         mockEntryTypeRequestRegular(
    //             EntryType.BIOSAMPLES.getName(), Map.of("id", List.of("Sample0002")));
    //    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    //
    //    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    //    JsonNode biosamples = queryEntryType.query(patientRegistry);
    //    assertEquals(1, biosamples.get("responseSummary").get("numTotalResults").intValue());
    //    assertTrue(biosamples.get("responseSummary").get("exists").booleanValue());
    //    JsonNode sample =
    // biosamples.get("response").get("resultSets").get(0).get("results").get(0);
    //    assertEquals("Sample0002", sample.get("id").asText());
    //    assertEquals("P3Y1M4D", sample.get("collectionMoment").asText());
  }
}
