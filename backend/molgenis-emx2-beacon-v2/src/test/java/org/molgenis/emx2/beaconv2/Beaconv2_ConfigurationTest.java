package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Configuration;
import org.molgenis.emx2.json.JsonUtil;
import spark.Request;
import spark.Response;

public class Beaconv2_ConfigurationTest {

  private Request mockRequest() {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");

    return request;
  }

  @Test
  public void testConfiguration() throws Exception {
    Configuration configuration = new Configuration();

    JsonNode result = configuration.getResponse(mockRequest(), mock(Response.class));
    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"meta\" : {"));

    // last line (except for closing braces)
    assertTrue(json.contains("\"label\" : \"Sequencing run\""));

    // check ids of all possible return types
    assertTrue(json.contains("\"id\" : \"Analyses\","));
    assertTrue(json.contains("\"id\" : \"Biosamples\","));
    assertTrue(json.contains("\"id\" : \"Cohorts\","));
    assertTrue(json.contains("\"id\" : \"Dataset\","));
    assertTrue(json.contains("\"id\" : \"GenomicVariations\","));
    assertTrue(json.contains("\"id\" : \"Individuals\","));
    assertTrue(json.contains("\"id\" : \"Runs\","));
  }
}
