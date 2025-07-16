package org.molgenis.emx2.datamodels.beacon.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

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
import org.molgenis.emx2.datamodels.TestLoaders;
import org.molgenis.emx2.datamodels.beacon.BeaconTestUtil;
import org.molgenis.emx2.graphql.GraphqlSession;

public class BeaconGenomicVariantTest extends TestLoaders {

  @Test
  public void testGenomicVariants_SequenceQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "referenceName", List.of("20"),
                "start", List.of("2447955"),
                "referenceBases", List.of("c"),
                "alternateBases", List.of("g")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode queryResult =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    JsonNode results = queryResult.get("response").get("resultSets").get(0).get("results");

    assertEquals(1, results.size());
    assertEquals("20:2447955..2447958c>g", results.get(0).get("variantInternalId").textValue());
  }

  @Test
  public void testGenomicVariants_NoHits() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "referenceName", List.of("20"),
                "start", List.of("2447955"),
                "referenceBases", List.of("c"),
                "alternateBases", List.of("a")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertFalse(result.get("responseSummary").get("exists").booleanValue());
  }

  @Test
  public void testGenomicVariants_RangeQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "start", List.of("2447952"),
                "end", List.of("2447955"),
                "referenceName", List.of("20")));

    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode queryResult =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertEquals(2, queryResult.get("responseSummary").get("numTotalResults").intValue());
    JsonNode results = queryResult.get("response").get("resultSets").get(0).get("results");

    assertTrue(results.get(0).get("variantInternalId").textValue().contains("20:244795"));
    assertTrue(results.get(1).get("variantInternalId").textValue().contains("20:244795"));
  }

  @Test
  public void testGenomicVariants_GeneIdQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            "g_variants", Map.of("geneId", List.of("SNRPB")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    assertEquals(3, result.get("responseSummary").get("numTotalResults").intValue());
  }

  @Test
  public void testGenomicVariants_BracketQuery() {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            "g_variants",
            Map.of(
                "start", List.of("2447945,2447951"),
                "end", List.of("2447952,2447953"),
                "referenceName", List.of("20")));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode queryResult =
        queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());

    JsonNode results = queryResult.get("response").get("resultSets").get(0).get("results");

    assertEquals(1, results.size());
    assertEquals("20:2447951..2447952c>g", results.get(0).get("variantInternalId").textValue());
  }

  @Test
  public void testPostGenomicVariant() throws Exception {
    Context request =
        BeaconTestUtil.mockEntryTypeRequestRegular(
            EntryType.GENOMIC_VARIANT.getName(), new HashMap<>());
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
    JsonNode json = queryEntryType.query(new GraphqlSession(ADMIN_USER), patientRegistry.getName());
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(3, results.size());
  }
}
