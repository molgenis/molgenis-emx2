package org.molgenis.emx2.beaconv2.vp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Map;
import spark.Request;

public class BeaconVpMapTest {

  private Request mockRequest() {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon_vp");
    when(request.attribute("specification")).thenReturn("beacon_vp");

    return request;
  }

  @Test
  public void testMap() {
    Map map = new Map(mockRequest());
    JsonNode result = map.getResponse();

    assertEquals("org.molgenis.beaconv2", result.get("meta").get("beaconId").textValue());
    assertEquals(
        "../../configuration/beaconMapSchema.json",
        result.get("response").get("$schema").textValue());
    assertEquals(
        "http://localhost:8080/api/beacon_vp/individuals",
        result.get("response").get("endpointSets").get("individuals").get("rootUrl").textValue());
    assertEquals(
        "http://localhost:8080/api/beacon_vp/individuals/{id}",
        result
            .get("response")
            .get("endpointSets")
            .get("individuals")
            .get("singleEntryUrl")
            .textValue());
    assertEquals(
        "http://localhost:8080/api/beacon_vp/individuals/filtering_terms",
        result
            .get("response")
            .get("endpointSets")
            .get("individuals")
            .get("filterTermsUrl")
            .textValue());
  }
}
