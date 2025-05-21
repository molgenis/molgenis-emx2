package org.molgenis.emx2.datamodels.beacon.vp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Map;

public class BeaconVpMapTest {

  private Context mockRequest() {
    Context request = mock(Context.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon_vp");
    when(request.attribute("specification")).thenReturn("beacon_vp");

    return request;
  }

  @Test
  @Disabled
  public void testMap() throws JsonProcessingException {
    Map map = new Map();
    Context context = mockRequest();
    map.getResponse(mockRequest());

    JsonNode result = new ObjectMapper().readTree(context.result());

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
