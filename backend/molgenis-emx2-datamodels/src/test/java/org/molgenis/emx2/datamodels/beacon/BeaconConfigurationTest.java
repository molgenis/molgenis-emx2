package org.molgenis.emx2.datamodels.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Configuration;

public class BeaconConfigurationTest {

  private Context mockRequest() {
    Context request = mock(Context.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");

    return request;
  }

  @Test
  public void testConfiguration() {
    Configuration configuration = new Configuration();

    Context context = mockRequest();

    JsonNode result = configuration.getResponse(context);
    assertTrue(result.has("meta"));

    JsonNode entryTypes = result.get("response").get("entryTypes");

    assertEquals(
        "Sequencing run",
        entryTypes.get("runs").get("ontologyTermForThisType").get("label").asText());

    assertEquals("IndividualAnalyses", entryTypes.get("analyses").get("id").asText());
    assertEquals("Biosamples", entryTypes.get("biosamples").get("id").asText());
    assertEquals("Cohorts", entryTypes.get("cohorts").get("id").asText());
    assertEquals("Datasets", entryTypes.get("datasets").get("id").asText());
    assertEquals("GenomicVariants", entryTypes.get("g_variants").get("id").asText());
    assertEquals("Individuals", entryTypes.get("individuals").get("id").asText());
    assertEquals("SequencingRuns", entryTypes.get("runs").get("id").asText());
  }
}
