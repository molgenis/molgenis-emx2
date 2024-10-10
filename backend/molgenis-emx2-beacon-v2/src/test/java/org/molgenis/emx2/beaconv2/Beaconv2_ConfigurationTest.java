package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Configuration;

public class Beaconv2_ConfigurationTest {

  private Context mockRequest() {
    Context request = mock(Context.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");

    return request;
  }

  @Test
  @Disabled
  public void testConfiguration() throws Exception {
    Configuration configuration = new Configuration();

    Context context = mockRequest();
    configuration.getResponse(mockRequest());

    String json = context.result();
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
