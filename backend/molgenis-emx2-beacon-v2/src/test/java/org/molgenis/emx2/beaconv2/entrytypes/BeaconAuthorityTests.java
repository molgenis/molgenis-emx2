package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import spark.Request;

public class BeaconAuthorityTests extends BeaconModelEndPointTest {

  @AfterAll
  public void after() {
    database.setActiveUser("VIEWER_TEST_USER");
  }

  @Test
  public void testRecordQueryAsViewerUser_fiveRecords() {
    database.setActiveUser("VIEWER_TEST_USER");
    Request request = mockEntryTypeRequest(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");

    assertEquals(5, results.size());
  }

  @Test
  public void testRecordQueryAsAggregateUser_noRecords() {
    database.setActiveUser("AGGREGATOR_TEST_USER");
    Request request = mockEntryTypeRequest(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);
    JsonNode resultsSets = json.get("response").get("resultSets");

    assertEquals(0, resultsSets.size());
  }

  @Test
  public void testRecordQueryAsExistsUser_noRecords() {
    database.setActiveUser("EXISTS_TEST_USER");
    Request request = mockEntryTypeRequest(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);
    JsonNode resultsSets = json.get("response").get("resultSets");

    assertEquals(0, resultsSets.size());
  }

  @Test
  public void testCountQueryAsExistsUser_noRecords() throws JsonProcessingException {
    database.setActiveUser("EXISTS_TEST_USER");
    JsonNode json =
        mockIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    assertFalse(json.get("responseSummary").get("exists").booleanValue());
    assertEquals(0, json.get("responseSummary").get("numTotalResults").intValue());
  }

  @Test
  public void testCountQueryAsAggregatorUser_tenResults() throws JsonProcessingException {
    database.setActiveUser("AGGREGATOR_TEST_USER");
    JsonNode json =
        mockIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    // Aggregator user is only allowed to see 10 when range is from 1-10
    assertEquals(10, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }

  @Test
  public void testExistsQueryAsExistsUser_true() throws JsonProcessingException {
    database.setActiveUser("EXISTS_TEST_USER");
    JsonNode json =
        mockIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    assertFalse(json.get("response").get("resultSets").get(0).has("resultsCount"));
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
  }

  @Test
  public void testExistsQueryAsAnonymousUser_false() throws JsonProcessingException {
    database.setActiveUser(ANONYMOUS);
    JsonNode json =
        mockIndividualsPostRequest(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    assertFalse(json.get("responseSummary").get("exists").booleanValue());
  }
}
