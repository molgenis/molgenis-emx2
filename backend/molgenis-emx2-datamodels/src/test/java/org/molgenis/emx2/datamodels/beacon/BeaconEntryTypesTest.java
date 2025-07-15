package org.molgenis.emx2.datamodels.beacon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.EntryTypes;

@Tag("slow")
public class BeaconEntryTypesTest {

  private Context mockRequest() {
    Context request = mock(Context.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");

    return request;
  }

  @Test
  public void testEntryTypes() {
    Context context = mockRequest();
    EntryTypes entryTypes = new EntryTypes();
    JsonNode result = entryTypes.getResponse(context);

    assertEquals("org.molgenis.beaconv2", result.get("meta").get("beaconId").textValue());
    assertEquals(
        "../../configuration/entryTypesSchema.json",
        result.get("response").get("$schema").textValue());

    JsonNode analysis = result.get("response").get("entryTypes").get("analyses");
    assertTrue(analysis.isObject());
    assertEquals("Analyses", analysis.get("id").textValue());
    assertEquals("analyses", analysis.get("name").textValue());
    assertEquals(
        "edam:operation_2945", analysis.get("ontologyTermForThisType").get("id").textValue());

    JsonNode biosample = result.get("response").get("entryTypes").get("biosamples");
    assertTrue(biosample.isObject());
    assertEquals("Biosamples", biosample.get("id").textValue());
    assertEquals("biosamples", biosample.get("name").textValue());
    assertEquals("NCIT:C70699", biosample.get("ontologyTermForThisType").get("id").textValue());

    JsonNode cohorts = result.get("response").get("entryTypes").get("cohorts");
    assertTrue(cohorts.isObject());
    assertEquals("Cohorts", cohorts.get("id").textValue());
    assertEquals("cohorts", cohorts.get("name").textValue());
    assertEquals("NCIT:C61512", cohorts.get("ontologyTermForThisType").get("id").textValue());

    JsonNode datasets = result.get("response").get("entryTypes").get("datasets");
    assertTrue(datasets.isObject());
    assertEquals("Datasets", datasets.get("id").textValue());
    assertEquals("datasets", datasets.get("name").textValue());
    assertEquals("NCIT:C47824", datasets.get("ontologyTermForThisType").get("id").textValue());

    JsonNode genomicVariants = result.get("response").get("entryTypes").get("g_variants");
    assertTrue(genomicVariants.isObject());
    assertEquals("GenomicVariants", genomicVariants.get("id").textValue());
    assertEquals("g_variants", genomicVariants.get("name").textValue());
    assertEquals(
        "ENSGLOSSARY:0000092",
        genomicVariants.get("ontologyTermForThisType").get("id").textValue());

    JsonNode individuals = result.get("response").get("entryTypes").get("individuals");
    assertTrue(individuals.isObject());
    assertEquals("Individuals", individuals.get("id").textValue());
    assertEquals("individuals", individuals.get("name").textValue());
    assertEquals("NCIT:C25190", individuals.get("ontologyTermForThisType").get("id").textValue());

    JsonNode runs = result.get("response").get("entryTypes").get("runs");
    assertTrue(runs.isObject());
    assertEquals("SequencingRuns", runs.get("id").textValue());
    assertEquals("runs", runs.get("name").textValue());
    assertEquals("NCIT:C148088", runs.get("ontologyTermForThisType").get("id").textValue());
  }
}
