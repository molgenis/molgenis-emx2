package org.molgenis.emx2.beaconv2.vp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.EntryTypes;
import spark.Request;

public class BeaconVpEntryTypesTest {

  private Request mockRequest() {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon_vp");
    when(request.attribute("specification")).thenReturn("beacon_vp");

    return request;
  }

  @Test
  public void testEntryTypes() {
    Request request = mockRequest();
    EntryTypes entryTypes = new EntryTypes(request);
    JsonNode result = entryTypes.getResponse();

    assertEquals("org.molgenis.beaconv2", result.get("meta").get("beaconId").textValue());
    assertEquals(
        "../../configuration/entryTypesSchema.json",
        result.get("response").get("$schema").textValue());

    JsonNode biosample = result.get("response").get("entryTypes").get("biosamples");
    assertTrue(biosample.isObject());
    assertEquals("Biosamples", biosample.get("id").textValue());
    assertEquals("biosamples", biosample.get("name").textValue());
    assertEquals("NCIT:C70699", biosample.get("ontologyTermForThisType").get("id").textValue());

    JsonNode datasets = result.get("response").get("entryTypes").get("catalogs");
    assertTrue(datasets.isObject());
    assertEquals("Dataset", datasets.get("id").textValue());
    assertEquals("catalogs", datasets.get("name").textValue());
    assertEquals("NCIT:C47824", datasets.get("ontologyTermForThisType").get("id").textValue());

    JsonNode individuals = result.get("response").get("entryTypes").get("individuals");
    assertTrue(individuals.isObject());
    assertEquals("Individuals", individuals.get("id").textValue());
    assertEquals("individuals", individuals.get("name").textValue());
    assertEquals("NCIT:C25190", individuals.get("ontologyTermForThisType").get("id").textValue());
  }
}
