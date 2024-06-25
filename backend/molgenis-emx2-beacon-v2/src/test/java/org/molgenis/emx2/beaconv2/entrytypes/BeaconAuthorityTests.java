package org.molgenis.emx2.beaconv2.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.Privileges.RANGE;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import spark.Request;

public class BeaconAuthorityTests extends BeaconModelEndPointTest {

  @BeforeAll
  public void setup() {
    super.setup();
    beaconSchema.addMember("VIEWER_TEST_USER", VIEWER.toString());
    beaconSchema.addMember("AGGREGATOR_TEST_USER", AGGREGATOR.toString());
    beaconSchema.addMember("COUNT_TEST_USER", COUNT.toString());
    beaconSchema.addMember("EXISTS_TEST_USER", EXISTS.toString());
    beaconSchema.addMember("RANGE_TEST_USER", RANGE.toString());
    beaconSchema.removeMember(ANONYMOUS);
    database.setActiveUser("VIEWER_TEST_USER");
  }

  @AfterAll
  public void after() {
    database.becomeAdmin();
    beaconSchema.addMember(ANONYMOUS, VIEWER.toString());
  }

  @Test
  public void testRecordQueryAsViewerUser_fiveRecords() {
    database.setActiveUser("VIEWER_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    Request request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(beaconSchema);
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");

    assertEquals(5, results.size());
  }

  @Test
  public void testRecordQueryAsAggregateUser_noRecords() {
    database.setActiveUser("AGGREGATOR_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    Request request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(beaconSchema);
    JsonNode resultsSets = json.get("response").get("resultSets");

    assertEquals(0, resultsSets.size());
  }

  @Test
  public void testCountQueryAsAggregatorUser_tenResults() throws JsonProcessingException {
    database.setActiveUser("AGGREGATOR_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    // Aggregator user is only allowed to see 10 when range is from 1-10
    assertEquals(10, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }

  @Test
  public void testCountQueryAsExistsUser_noRecords()
      throws JsonProcessingException, InterruptedException {
    database.setActiveUser("EXISTS_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertFalse(json.get("responseSummary").get("exists").booleanValue());
    assertEquals(0, json.get("responseSummary").get("numTotalResults").intValue());
  }

  @Test
  public void testExistsQueryAsExistsUser_true() throws JsonProcessingException {
    database.setActiveUser("EXISTS_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    assertFalse(json.get("response").get("resultSets").get(0).has("resultsCount"));
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
  }

  @Test
  public void testRecordQueryAsExistsUser_noRecords() {
    database.setActiveUser("EXISTS_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    Request request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(beaconSchema);
    JsonNode resultsSets = json.get("response").get("resultSets");

    assertEquals(0, resultsSets.size());
  }

  @Test
  public void testExistsQueryAsAnonymousUser_false() throws JsonProcessingException {
    database.setActiveUser(ANONYMOUS);
    beaconSchema = database.getSchema("fairdatahub");
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertFalse(json.get("responseSummary").get("exists").booleanValue());
  }

  @Test
  public void testCountQueryAsRangeUser_range() throws JsonProcessingException {
    database.setActiveUser("RANGE_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    BeaconRequestBody beaconRequestBody =
        mockIndividualsPostRequestRegular(
            """
              {
                "query": {
                  "requestedGranularity": "count"
                }
              }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequestBody);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    JsonNode resultSet = json.get("response").get("resultSets").get(0);
    assertTrue(resultSet.has("resultsCount"));
    assertEquals(10, resultSet.get("resultsCount").intValue());
    assertEquals(
        10, resultSet.get("info").get("resultCountDescription").get("maxRange").intValue());
    assertEquals(1, resultSet.get("info").get("resultCountDescription").get("minRange").intValue());
  }

  @Test
  public void testCountQueryAsCountUser_fiveResults() throws JsonProcessingException {
    database.setActiveUser("COUNT_TEST_USER");
    beaconSchema = database.getSchema("fairdatahub");
    BeaconRequestBody beaconRequestBody =
        mockIndividualsPostRequestRegular(
            """
              {
                "query": {
                  "requestedGranularity": "count"
                }
              }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequestBody);
    JsonNode json = queryEntryType.query(beaconSchema);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    JsonNode resultSet = json.get("response").get("resultSets").get(0);
    assertTrue(resultSet.has("resultsCount"));
    assertEquals(5, resultSet.get("resultsCount").intValue());
  }
}
