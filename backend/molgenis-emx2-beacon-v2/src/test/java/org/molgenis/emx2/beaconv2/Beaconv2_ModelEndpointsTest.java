package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.*;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.json.JsonUtil;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.QueryParamsMap;
import spark.Request;

@Tag("slow")
public class Beaconv2_ModelEndpointsTest {

  static Database database;
  static Schema beaconSchema;
  static List<Table> tables;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
    // beaconSchema = database.getSchema("fairdatahub");
    beaconSchema = database.dropCreateSchema("fairdatahub");
    ProfileLoader b2l = new ProfileLoader("_profiles/FAIRDataHub.yaml");
    b2l.load(beaconSchema, true);
    tables = List.of(beaconSchema.getTable("Individuals"));
  }

  @Test
  void testFilteringTerms() throws Exception {
    FilteringTerms filteringTerms = new FilteringTerms(database);
    String json = JsonUtil.getWriter().writeValueAsString(filteringTerms);
    assertTrue(json.contains("\"entityType\" : \"filteringterms\""));
    assertTrue(json.contains("\"filteringTerms\" : ["));
    assertTrue(json.contains("\"type\" : \"alphanumeric\","));
    assertTrue(json.contains("\"id\" : \"position_assemblyId\","));
    assertTrue(json.contains("\"scope\" : \"genomicVariations\""));
    assertTrue(json.contains("\"type\" : \"ontology\","));
    assertTrue(json.contains("\"id\" : \"NCIT:C124261\","));
    assertTrue(json.contains("\"label\" : \"Whole Transcriptome Sequencing\","));
    assertTrue(json.contains("\"scope\" : \"runs\""));
  }

  private Request mockEntryTypeRequest(String entryType, Map<String, String[]> queryParams) {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");
    Map<String, String> urlParams = Map.of(":entry_type", entryType);
    when(request.params()).thenReturn(urlParams);
    when(request.queryMap()).thenReturn(Mockito.mock(QueryParamsMap.class));
    when(request.queryMap().toMap()).thenReturn(queryParams);

    return request;
  }

  @Test
  public void testGenomicVariants_SequenceQuery() throws Exception {
    Request request =
        mockEntryTypeRequest(
            "g_variants",
            Map.of(
                "referenceName", new String[] {"20"},
                "start", new String[] {"2447955"},
                "referenceBases", new String[] {"c"},
                "alternateBases", new String[] {"g"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
  }

  @Test
  public void testGenomicVariants_NoHits() throws Exception {
    Request request =
        mockEntryTypeRequest(
            "g_variants",
            Map.of(
                "referenceName", new String[] {"20"},
                "start", new String[] {"2447955"},
                "referenceBases", new String[] {"c"},
                "alternateBases", new String[] {"a"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(result);

    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
    assertFalse(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertFalse(json.contains("\"resultsCount\" : 1,"));
  }

  @Test
  public void testGenomicVariants_RangeQuery() throws Exception {
    Request request =
        mockEntryTypeRequest(
            "g_variants",
            Map.of(
                "start", new String[] {"2447952"},
                "end", new String[] {"2447955"},
                "referenceName", new String[] {"20"}));

    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"resultsCount\" : 2,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
  }

  @Test
  public void testGenomicVariants_GeneIdQuery() throws Exception {
    Request request = mockEntryTypeRequest("g_variants", Map.of("geneId", new String[] {"SNRPB"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(database);

    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"resultsCount\" : 3,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447955..2447958c>g\","));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447946..2447950c>g\","));
    assertTrue(json.contains("\"id\" : \"Orphanet:391665\""));
    assertTrue(json.contains("clinicalRelevance"));
  }

  @Test
  public void testGenomicVariants_BracketQuery() throws Exception {
    Request request =
        mockEntryTypeRequest(
            "g_variants",
            Map.of(
                "start", new String[] {"2447945,2447951"},
                "end", new String[] {"2447952,2447953"},
                "referenceName", new String[] {"20"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);
    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode result = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(result);
    assertTrue(json.contains("\"resultsCount\" : 1,"));
    assertTrue(json.contains("\"variantInternalId\" : \"20:2447951..2447952c>g\","));
  }

  @Test
  public void testAnalyses_NoParams() throws Exception {
    Request request = mockEntryTypeRequest("analyses", new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"resultsCount\" : 5,"));
  }

  @Test
  public void testAnalyses_NoHits() throws Exception {
    Request request =
        mockEntryTypeRequest(EntryType.ANALYSES.getId(), Map.of("id", new String[] {"A05"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
  }

  @Test
  public void testAnalyses_IdQuery() throws Exception {
    Request request =
        mockEntryTypeRequest(EntryType.ANALYSES.getId(), Map.of("id", new String[] {"A03"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode analyses = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(analyses);
    assertTrue(json.contains("\"id\" : \"A03\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
  }

  @Test
  public void testBiosamples_NoParams() throws Exception {
    Request request = mockEntryTypeRequest(EntryType.BIOSAMPLES.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);

    assertTrue(json.contains("\"resultsCount\" : 3,"));
    assertTrue(json.contains("obtentionProcedure"));
    assertTrue(json.contains("procedureCode"));
    assertTrue(json.contains("\"id\" : \"OBI:0002654\""));
    assertTrue(json.contains("\"label\" : \"needle biopsy\""));
  }

  @Test
  public void testBiosamples_NoHits() throws Exception {
    Request request =
        mockEntryTypeRequest(
            EntryType.BIOSAMPLES.getId(), Map.of("id", new String[] {"Sample0003"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);

    assertTrue(json.contains("\"response\" : {\n" + "    \"resultSets\" : [ ]"));
  }

  @Test
  public void testBiosamples_IdQuery() throws Exception {
    Request request =
        mockEntryTypeRequest(
            EntryType.BIOSAMPLES.getId(), Map.of("id", new String[] {"Sample0002"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode biosamples = queryEntryType.query(database);
    String json = JsonUtil.getWriter().writeValueAsString(biosamples);
    assertTrue(json.contains("\"id\" : \"Sample0002\","));
    assertTrue(json.contains("\"resultsCount\" : 1,"));
  }

  @Test
  public void testCohorts_NoParams() throws Exception {
    Request request = mockEntryTypeRequest(EntryType.COHORTS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode cohorts = queryEntryType.query(database);

    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    assertTrue(json.contains("\"id\" : \"Cohort0001\""));
    assertTrue(json.contains("\"id\" : \"ISO3166:FR\""));
  }

  @Test
  public void testCohorts_NoHits() throws Exception {
    Request request =
        mockEntryTypeRequest(EntryType.COHORTS.getId(), Map.of("id", new String[] {"Cohort0003"}));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode cohorts = queryEntryType.query(database);

    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    assertTrue(json.contains("\"collections\" : [ ]"));
  }

  @Test
  public void testIndividuals_NoParams() {
    Request request = mockEntryTypeRequest(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(5, results.size());
  }

  @Test
  public void testIndividuals_pathIdQuery_oneResult() {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    Map<String, String> urlParams =
        Map.of(":entry_type", EntryType.INDIVIDUALS.getId(), ":id", "Ind001");

    when(request.params()).thenReturn(urlParams);
    when(request.queryMap()).thenReturn(mock(QueryParamsMap.class));
    when(request.queryMap().toMap()).thenReturn(new HashMap<>());
    when(request.attribute("specification")).thenReturn("beacon");

    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(1, results.size());
    assertEquals(results.get(0).get("id").textValue(), "Ind001");
  }

  @Test
  public void testRunsOfIndividual_pathQuery_twoResults() {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    Map<String, String> urlParams =
        Map.of(
            ":entry_type_id", EntryType.INDIVIDUALS.getId(),
            ":entry_type", EntryType.RUNS.getId(),
            ":id", "Ind001");

    when(request.params()).thenReturn(urlParams);
    when(request.queryMap()).thenReturn(mock(QueryParamsMap.class));
    when(request.queryMap().toMap()).thenReturn(new HashMap<>());
    when(request.attribute("specification")).thenReturn("beacon");

    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);

    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals(results.get(0).get("individualId").textValue(), "Ind001");
    assertEquals(results.get(1).get("individualId").textValue(), "Ind001");
  }

  private JsonNode doIndividualsPostRequest(String body) throws JsonProcessingException {
    Request request = mockEntryTypeRequest(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(body, BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);

    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    return queryEntryType.query(database);
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_NoHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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

    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnGenderAtBirth_ignoreFilter() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    assertEquals("[NCIT_C28421]", json.get("info").get("unsupportedFilters").textValue());
    assertEquals(5, json.get("responseSummary").get("numTotalResults").intValue());
    assertEquals(true, json.get("responseSummary").get("exists").booleanValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_OntologyFilterSyntax_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    //    assertTrue(jsonString.contains("\"exists\" : true"));
    //    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_AlsoOneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_TwoHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 2"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnDisease_NoHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAge_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAge_NoHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeGreaterThan_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_ThreeHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 3"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_TwoHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                          	"filters": [
                          	  {
                          		"id": "ncit:C83164",
                          		"value": 34,
                          		"operator": "<"
                          	  }
                          	]
                            }
                          }""");
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 2"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThanOrEquals_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeLessThan_NoHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnset_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    //    assertTrue(json.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnset_NoHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThan_TwoHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 2"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThan_NoHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : false"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 0"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeOfOnsetGreaterThanOrEquals_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosis_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosisLessThan_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnAgeAtDiagnosisUnsupportedFilter() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    assertEquals("[ncit:C15642]", json.get("info").get("unsupportedFilters").textValue());
    assertEquals(5, json.get("responseSummary").get("numTotalResults").intValue());
    assertEquals(true, json.get("responseSummary").get("exists").booleanValue());
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_asArray_OneHit() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 1"));
  }

  @Test
  public void test_EJP_RD_VP_API_FilterOnCausalGenes_TwoHits() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
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
    String jsonString = JsonUtil.getWriter().writeValueAsString(json);
    assertTrue(jsonString.contains("\"exists\" : true"));
    assertTrue(jsonString.contains("\"numTotalResults\" : 2"));
  }

  @Test
  public void testRequestedGranularity_requestBoolean() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
    assertNull(json.get("response").get("resultSets").get(0).get("resultsCount"));
  }

  @Test
  public void testRequestedGranularity_requestCount() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    assertEquals(5, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }

  @Test
  public void testPagination_TwoItems_Offset0() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                              "pagination": {
                                "limit": 2,
                                "skip": 0
                              }
                            }
                          }""");
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals("Ind001", results.get(0).get("id").textValue());
    assertEquals("Ind002", results.get(1).get("id").textValue());
  }

  @Test
  public void testPagination_TwoItems_Offset2() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                              "pagination": {
                                "limit": 2,
                                "skip": 2
                              }
                            }
                          }""");
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(2, results.size());
    assertEquals("MinIndNoRefs003", results.get(0).get("id").textValue());
    assertEquals("MinInd004", results.get(1).get("id").textValue());
  }

  @Test
  public void testPagination_LimitZero_AllResult() throws Exception {
    JsonNode json =
        doIndividualsPostRequest(
            """
                          {
                            "query": {
                              "pagination": {
                                "limit": 0,
                                "skip": 0
                              }
                            }
                          }""");
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(5, results.size());
  }

  @Test
  public void testPostGenomicVariant() throws Exception {
    Request request = mockEntryTypeRequest(EntryType.GENOMIC_VARIANT.getName(), new HashMap<>());
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
    JsonNode json = queryEntryType.query(database);
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");
    assertEquals(3, results.size());
  }
}
