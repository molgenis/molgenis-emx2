package org.molgenis.emx2.datamodels.beacon.entrytypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.datamodels.beacon.BeaconTestUtil.mockEntryTypeRequestRegular;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.TestLoaders;
import org.molgenis.emx2.graphql.GraphqlSession;

public class BeaconBiosamplesTests extends TestLoaders {

  @Test
  public void testBiosamples_NoParams() {
    Context request = mockEntryTypeRequestRegular(EntryType.BIOSAMPLES.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertEquals(20, biosamples.get("responseSummary").get("numTotalResults").intValue());
  }

  @Test
  public void testBiosamples_NoHits() {
    Context request =
        mockEntryTypeRequestRegular(
            EntryType.BIOSAMPLES.getId(), Map.of("id", List.of("Sample0003")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertEquals(0, biosamples.get("responseSummary").get("numTotalResults").intValue());
    assertFalse(biosamples.get("responseSummary").get("exists").booleanValue());
  }

  @Test
  public void testBiosamples_IdQuery() {
    Context request =
        mockEntryTypeRequestRegular(
            EntryType.BIOSAMPLES.getId(), Map.of("id", List.of("Sample0002")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertEquals(1, biosamples.get("responseSummary").get("numTotalResults").intValue());
    assertTrue(biosamples.get("responseSummary").get("exists").booleanValue());
    JsonNode sample = biosamples.get("response").get("resultSets").get(0).get("results").get(0);
    assertEquals("Sample0002", sample.get("id").asText());
    assertEquals("P3Y1M4D", sample.get("collectionMoment").asText());
  }
}
