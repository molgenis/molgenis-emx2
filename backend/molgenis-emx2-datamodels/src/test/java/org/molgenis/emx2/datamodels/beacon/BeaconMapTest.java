package org.molgenis.emx2.datamodels.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Map;

public class BeaconMapTest {

  private Context mockRequest() {
    Context request = mock(Context.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");

    return request;
  }

  @Test
  public void testMap() {
    Map map = new Map();
    JsonNode result = map.getResponse(mockRequest());

    assertEquals("org.molgenis.beaconv2", result.get("meta").get("beaconId").textValue());
    assertEquals(
        "../../configuration/beaconMapSchema.json",
        result.get("response").get("$schema").textValue());
    assertEquals(
        "http://localhost:8080/api/beacon/analyses",
        result.get("response").get("endpointSets").get("analyses").get("rootUrl").textValue());
    assertEquals(
        "http://localhost:8080/api/beacon/analyses/{id}",
        result
            .get("response")
            .get("endpointSets")
            .get("analyses")
            .get("singleEntryUrl")
            .textValue());
    assertEquals(
        "http://localhost:8080/api/beacon/analyses/filtering_terms",
        result
            .get("response")
            .get("endpointSets")
            .get("analyses")
            .get("filterTermsUrl")
            .textValue());
  }
}
