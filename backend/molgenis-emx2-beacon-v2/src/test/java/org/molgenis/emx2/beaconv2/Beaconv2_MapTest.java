package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Map;

public class Beaconv2_MapTest {

  private Context mockRequest() {
    Context request = mock(Context.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");

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
