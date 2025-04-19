package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.json.JsonUtil;

public class BeaconGenomicVariantTest extends BeaconModelEndPointTest {

  @Test
  public void testGenomicVariants_SequenceQuery() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "referenceName", List.of("20"),
                "start", List.of("2447955"),
                "referenceBases", List.of("c"),
                "alternateBases", List.of("g")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(beaconSchema);
    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
  }

  @Test
  public void testGenomicVariants_NoHits() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "referenceName", List.of("20"),
                "start", List.of("2447955"),
                "referenceBases", List.of("c"),
                "alternateBases", List.of("a")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(beaconSchema);
    String json = JsonUtil.getWriter().writeValueAsString(result);

    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertFalse(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertFalse(json.contains("\"resultsCount\" : 1,"));
  }

  @Test
  public void testGenomicVariants_RangeQuery() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "start", List.of("2447952"),
                "end", List.of("2447955"),
                "referenceName", List.of("20")));

    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(beaconSchema);
    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"resultsCount\" : 2,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
  }

  @Test
  public void testGenomicVariants_GeneIdQuery() throws Exception {
    Context request = mockEntryTypeRequestRegular("g_variants", Map.of("geneId", List.of("SNRPB")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(beaconSchema);

    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"resultsCount\" : 3,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447946..2447950c>g\","));
    // assertTrue(json.contains("\"id\" : \"Orphanet:391665\""));
    // assertTrue(json.contains("clinicalRelevance"));
  }

  @Test
  public void testGenomicVariants_BracketQuery() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "start", List.of("2447945,2447951"),
                "end", List.of("2447952,2447953"),
                "referenceName", List.of("20")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(beaconSchema);
    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
  }

  @Test
  public void testPostGenomicVariant() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(EntryType.GENOMIC_VARIANT.getName(), new HashMap<>());
    ObjectMapper mapper = new ObjectMapper();
    String body =
        """
        {
          "query": {
            "requestParameters": {
              "geneId": "SNORD119"
            },
            "filters": [],
            "includeResultsetResponses": "ALL",
            "pagination": {
            "skip": 0,
              "limit": 0
            },
            "testMode": false,
            "requestedGranularity": "record"
          }
        }""";
    BeaconRequestBody beaconRequest = mapper.readValue(body, BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(3, results.size());
  }
}
