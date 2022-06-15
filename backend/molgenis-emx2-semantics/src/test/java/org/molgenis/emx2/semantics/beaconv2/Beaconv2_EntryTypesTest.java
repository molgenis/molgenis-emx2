package org.molgenis.emx2.semantics.beaconv2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.molgenis.emx2.beaconv2.endpoints.EntryTypes;
import org.molgenis.emx2.json.JsonUtil;
import spark.Request;

public class Beaconv2_EntryTypesTest {

  @Test
  public void testEntryTypes() throws Exception {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/myEmx2Schema/api/beacon/map");
    EntryTypes et = new EntryTypes();
    String json = JsonUtil.getWriter().writeValueAsString(et);

    // header
    assertTrue(
        json.contains(
            "  \"meta\" : {\n"
                + "    \"beaconId\" : \"org.molgenis.beaconv2\",\n"
                + "    \"apiVersion\" : \"v2.0.0-draft.4\",\n"
                + "    \"$schema\" : \"../beaconInfoResponse.json\","));

    // returned schema in response
    assertTrue(
        json.contains(
            "    \"returnedSchemas\" : [\n" + "      {\n" + "        \"entityType\" : \"entry\","));
    assertTrue(
        json.contains(
            "  \"response\" : {\n"
                + "    \"$schema\" : \"../../configuration/entryTypesSchema.json\","));

    // entry types
    assertTrue(
        json.contains(
            "      \"analysis\" : {\n"
                + "        \"id\" : \"analysis\",\n"
                + "        \"name\" : \"Bioinformatics analysis\","));
    assertTrue(
        json.contains(
            "      \"biosample\" : {\n"
                + "        \"id\" : \"biosample\",\n"
                + "        \"name\" : \"Biological Sample\","));
    assertTrue(
        json.contains(
            "      \"cohort\" : {\n"
                + "        \"id\" : \"cohort\",\n"
                + "        \"name\" : \"Cohort\","));
    assertTrue(
        json.contains(
            "      \"dataset\" : {\n"
                + "        \"id\" : \"dataset\",\n"
                + "        \"name\" : \"Dataset\","));
    assertTrue(
        json.contains(
            "      \"genomicVariant\" : {\n"
                + "        \"id\" : \"genomicVariant\",\n"
                + "        \"name\" : \"Genomic Variants\","));
    assertTrue(
        json.contains(
            "      \"individual\" : {\n"
                + "        \"id\" : \"individual\",\n"
                + "        \"name\" : \"Individual\","));
    assertTrue(
        json.contains(
            "      \"run\" : {\n"
                + "        \"id\" : \"run\",\n"
                + "        \"name\" : \"Sequencing run\","));

    // ontologies
    assertTrue(json.contains("\"id\" : \"edam:operation_2945\","));
    assertTrue(json.contains("\"id\" : \"NCIT:C70699\","));
    assertTrue(json.contains("\"id\" : \"NCIT:C61512\","));
    assertTrue(json.contains("\"id\" : \"NCIT:C47824\","));
    assertTrue(json.contains("\"id\" : \"ENSGLOSSARY:0000092\","));
    assertTrue(json.contains("\"id\" : \"NCIT:C25190\","));
    assertTrue(json.contains("\"id\" : \"NCIT:C148088\","));

    assertEquals(5369, json.length());
  }
}
