package org.molgenis.emx2.semantics.beaconv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.EntryTypes;
import org.molgenis.emx2.json.JsonUtil;
import spark.Request;

@Tag("slow")
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
            """
                                "meta" : {
                                  "beaconId" : "org.molgenis.beaconv2",
                                  "apiVersion" : "v2.0.0-draft.4",
                                  "$schema" : "../beaconInfoResponse.json","""
                .indent(2)));

    // returned schema in response
    assertTrue(
        json.contains(
            """
                                "returnedSchemas" : [
                                  {
                                    "entityType" : "entry","""
                .indent(4)));
    assertTrue(
        json.contains(
            "  \"response\" : {\n"
                + "    \"$schema\" : \"../../configuration/entryTypesSchema.json\","));

    // entry types
    assertTrue(
        json.contains(
            """
                                "analysis" : {
                                  "id" : "analysis",
                                  "name" : "Bioinformatics analysis","""
                .indent(6)));
    assertTrue(
        json.contains(
            """
                                "biosample" : {
                                  "id" : "biosample",
                                  "name" : "Biological Sample","""
                .indent(6)));
    assertTrue(
        json.contains(
            """
                                "cohort" : {
                                  "id" : "cohort",
                                  "name" : "Cohort","""
                .indent(6)));
    assertTrue(
        json.contains(
            """
                                "dataset" : {
                                  "id" : "dataset",
                                  "name" : "Dataset","""
                .indent(6)));
    assertTrue(
        json.contains(
            """
                                "genomicVariant" : {
                                  "id" : "genomicVariant",
                                  "name" : "Genomic Variants","""
                .indent(6)));
    assertTrue(
        json.contains(
            """
                                "individual" : {
                                  "id" : "individual",
                                  "name" : "Individual","""
                .indent(6)));
    assertTrue(
        json.contains(
            """
                                "run" : {
                                  "id" : "run",
                                  "name" : "Sequencing run","""
                .indent(6)));

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
