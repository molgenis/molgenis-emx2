package org.molgenis.emx2.datamodels.beacon.entrytypes;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Privileges.*;
import static org.molgenis.emx2.Privileges.RANGE;
import static org.molgenis.emx2.datamodels.beacon.BeaconTestUtil.mockEntryTypeRequestRegular;
import static org.molgenis.emx2.datamodels.beacon.BeaconTestUtil.mockIndividualsPostRequestRegular;
import static org.molgenis.emx2.sql.SqlDatabase.ANONYMOUS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.TestLoaders;

class BeaconAuthorityTests extends TestLoaders {

  static final String VIEWER_TEST_USER = "VIEWER_TEST_USER";
  static final String AGGREGATOR_TEST_USER = "AGGREGATOR_TEST_USER";
  static final String COUNT_TEST_USER = "COUNT_TEST_USER";
  static final String EXISTS_TEST_USER = "EXISTS_TEST_USER";
  static final String RANGE_TEST_USER = "RANGE_TEST_USER";

  @BeforeAll
  public void setup() {
    super.setup();
    patientRegistry.addMember(VIEWER_TEST_USER, VIEWER.toString());
    patientRegistry.addMember(AGGREGATOR_TEST_USER, AGGREGATOR.toString());
    patientRegistry.addMember(COUNT_TEST_USER, COUNT.toString());
    patientRegistry.addMember(EXISTS_TEST_USER, EXISTS.toString());
    patientRegistry.addMember(RANGE_TEST_USER, RANGE.toString());
    patientRegistry.removeMember(ANONYMOUS);
    database.setActiveUser(VIEWER_TEST_USER);
  }

  @AfterAll
  public void after() {
    database.becomeAdmin();
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    patientRegistry.addMember(ANONYMOUS, VIEWER.toString());
  }

  @Test
  void testRecordQueryAsViewerUser_tenRecords() {
    database.setActiveUser(VIEWER_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    Context request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(patientRegistry);
    JsonNode results = json.get("response").get("resultSets").get(0).get("results");

    assertEquals(10, results.size());
  }

  @Test
  void queryRestoresSharedDatabase() {
    database.setActiveUser(VIEWER_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    assertFalse(database.isAdmin());

    Context request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    QueryEntryType queryEntryType = new QueryEntryType(new BeaconRequestBody(request));
    queryEntryType.query(patientRegistry);

    assertEquals(VIEWER_TEST_USER, database.getActiveUser());
  }

  @Test
  void templateLookupRunsInsideTransaction() {
    database.becomeAdmin();
    Schema spySchema = Mockito.spy(database.getSchema(PATIENT_REGISTRY));
    Database spyDatabase = Mockito.spy(spySchema.getDatabase());
    Mockito.doReturn(spyDatabase).when(spySchema).getDatabase();

    Context request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    new QueryEntryType(new BeaconRequestBody(request)).query(spySchema);

    Mockito.verify(spyDatabase, Mockito.atLeastOnce()).tx(Mockito.any());
  }

  @Test
  void testRecordQueryAsAggregateUser_noRecords() {
    database.setActiveUser(AGGREGATOR_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    Context request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(patientRegistry);
    JsonNode resultsSets = json.get("response").get("resultSets");

    assertEquals(0, resultsSets.size());
  }

  @Test
  void testCountQueryAsAggregatorUser() throws JsonProcessingException {
    database.setActiveUser(AGGREGATOR_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(patientRegistry);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    assertEquals(23, json.get("response").get("resultSets").get(0).get("resultsCount").intValue());
    assertNull(json.get("response").get("resultSets").get(0).get("results"));
  }

  @Test
  void testCountQueryAsExistsUser_noRecords() throws JsonProcessingException, InterruptedException {
    database.setActiveUser(EXISTS_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "count"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(patientRegistry);
    assertFalse(json.get("responseSummary").get("exists").booleanValue());
    assertEquals(0, json.get("responseSummary").get("numTotalResults").intValue());
  }

  @Test
  void testExistsQueryAsExistsUser_true() throws JsonProcessingException {
    database.setActiveUser(EXISTS_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    BeaconRequestBody beaconRequest =
        mockIndividualsPostRequestRegular(
            """
                          {
                            "query": {
                              "requestedGranularity": "boolean"
                            }
                          }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequest);
    JsonNode json = queryEntryType.query(patientRegistry);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    assertFalse(json.get("response").get("resultSets").get(0).has("resultsCount"));
    assertTrue(json.get("response").get("resultSets").get(0).get("exists").booleanValue());
  }

  @Test
  void testRecordQueryAsExistsUser_noRecords() {
    database.setActiveUser(EXISTS_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    Context request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(patientRegistry);
    JsonNode resultsSets = json.get("response").get("resultSets");

    assertEquals(0, resultsSets.size());
  }

  @Test
  void testCountQueryAsRangeUser_range() throws JsonProcessingException {
    database.setActiveUser(RANGE_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    BeaconRequestBody beaconRequestBody =
        mockIndividualsPostRequestRegular(
            """
              {
                "query": {
                  "requestedGranularity": "count"
                }
              }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequestBody);
    JsonNode json = queryEntryType.query(patientRegistry);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    JsonNode resultSet = json.get("response").get("resultSets").get(0);
    assertTrue(resultSet.has("resultsCount"));
    assertEquals(30, resultSet.get("resultsCount").intValue());
    assertEquals(
        30, resultSet.get("info").get("resultCountDescription").get("maxRange").intValue());
    assertEquals(
        21, resultSet.get("info").get("resultCountDescription").get("minRange").intValue());
  }

  @Test
  void testCountQueryAsCountUser_fiveResults() throws JsonProcessingException {
    database.setActiveUser(COUNT_TEST_USER);
    patientRegistry = database.getSchema(PATIENT_REGISTRY);
    BeaconRequestBody beaconRequestBody =
        mockIndividualsPostRequestRegular(
            """
              {
                "query": {
                  "requestedGranularity": "count"
                }
              }""");
    QueryEntryType queryEntryType = new QueryEntryType(beaconRequestBody);
    JsonNode json = queryEntryType.query(patientRegistry);
    assertTrue(json.get("responseSummary").get("exists").booleanValue());
    JsonNode resultSet = json.get("response").get("resultSets").get(0);
    assertTrue(resultSet.has("resultsCount"));
    assertEquals(23, resultSet.get("resultsCount").intValue());
  }
}
