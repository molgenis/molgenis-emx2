package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Info;
import spark.Request;
import spark.Response;

public class Beaconv2_InfoTest {

  private Request mockRequest() {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");

    return request;
  }

  @Test
  public void testInfoRootEndpoint() {
    Info info = new Info();
    JsonNode respons = info.getResponse(mockRequest(), mock(Response.class));

    assertEquals(
        "Genomics Coordination Center",
        respons.get("response").get("organization").get("name").asText());
    assertEquals(
        "MOLGENIS EMX2 Beacon v2 at http://localhost:8080",
        respons.get("response").get("name").asText());
  }
}
