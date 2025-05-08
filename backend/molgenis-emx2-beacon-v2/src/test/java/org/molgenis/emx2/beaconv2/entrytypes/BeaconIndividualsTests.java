package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.json.JsonUtil;

@Disabled
public class BeaconIndividualsTests extends BeaconModelEndPointTest {

  @Test
  public void testIndividuals_NoParams() {
    Context request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(5, results.size());
  }

  @Test
  public void testIndividuals_pathIdQuery_oneResult() {
    Context request = mock(Context.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    Map<String, String> urlParams =
        Map.of("entry_type", EntryType.INDIVIDUALS.getId(), ":id", "Ind001");

    when(request.pathParamMap()).thenReturn(urlParams);
    when(request.queryParamMap()).thenReturn(new HashMap<>());
    when(request.attribute("specification")).thenReturn("beacon");

    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(1, results.size());
    assertEquals(results.get(0).get("id").textValue(), "Ind001");
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C28421",
                          		"value": "ncit:C16576",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_NoHits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                                "id": "ncit:C28421",
                                "value": "ncit:C16576",
                                "operator": "="
                          	  },
                          	  {
                                "id": "ncit:C28421",
                                "value": "ncit:C20197",
                                "operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void FilterOnGenderAtBirth_MultipleTerms_twoResult() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "filters": [
                                {
                                  "id": "NCIT:C28421",
                                  "value": ["GSSO_000124", "GSSO_000123"],
                                  "operator": "="
                                }
                              ]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(2, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void FilterOnGenderAtBirthGssoTerm_oneResult() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "NCIT:C28421",
                          		"value": "GSSO_000123",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_ignoreFilter() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "NCIT_C28421",
                          		"value": "ncit:C16577",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertEquals("[NCIT_C28421]", json.get("info").get("unsupportedFilters").textValue());
    assertTrue(json.get("responseSummary").get("exists").booleanValue());

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(5, results.size());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C2991",
                          		"value": "Orphanet_1895",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_OntologyFilterSyntax_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "Orphanet_1895"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    //    assertTrue(jsonString.contains("\"exists\" : true"));
    //    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_AlsoOneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C2991",
                          		"value": "Orphanet_1895",
                          		"operator": "="
                          	  },
                          	  {
                          		"id": "ncit:C2991",
                          		"value": "Orphanet_1955",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_TwoHits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C2991",
                          		"value": "Orphanet_1955",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(2, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_NoHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C2991",
                          		"value": "Orphanet_18730",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAge_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 32,
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAge_NoHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 30,
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeGreaterThan_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 33,
                          		"operator": ">"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_ThreeHits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 50,
                          		"operator": "<"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(3, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_THits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 33,
                          		"operator": "<"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(2, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThanOrEquals_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 2,
                          		"operator": "<="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_NoHits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 2,
                          		"operator": "<"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnset_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "",
                          		"value": 3,
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    //    assertTrue(json.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnset_NoHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C124353",
                          		"value": 91,
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThan_TwoHits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C124353",
                          		"value": 25,
                          		"operator": ">"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(2, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThan_NoHits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C124353",
                          		"value": 89,
                          		"operator": ">"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThanOrEquals_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C124353",
                          		"value": 89,
                          		"operator": ">="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosis_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C156420",
                          		"value": 2,
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosisLessThan_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C156420",
                          		"value": 50,
                          		"operator": "<"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosisUnsupportedFilter() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C15642",
                          		"value": 50,
                          		"operator": "<"
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertEquals("[ncit:C15642]", json.get("info").get("unsupportedFilters").textValue());
    assertTrue(json.get("responseSummary").get("exists").booleanValue());

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(5, results.size());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "edam:data_2295",
                          		"value": "TTN",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_asArray_OneHit() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "edam:data_2295",
                          		"value": ["TTN"],
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(1, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_TwoHits() throws Exception {
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestVp(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "edam:data_2295",
                          		"value": "COL7A1",
                          		"operator": "="
                          	  }
                          	]
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);

    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertEquals(2, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
  }
}
