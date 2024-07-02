package org.molgenis.emx2.beaconv2.entrytypes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.beaconv2.BeaconSpec.BEACON_V2;
import static org.molgenis.emx2.beaconv2.BeaconSpec.BEACON_VP;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.QueryParamsMap;
import spark.Request;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeaconModelEndPointTest {

  public static final String TEST_URL = "http://localhost:8080/api/";
  public static final String SCHEMA_NAME = "fairdatahub";

  protected static Database database;
  protected static Schema beaconSchema;

  @BeforeAll
  public void setup() {
    if (database == null) {
      database = TestDatabaseFactory.getTestDatabase();
      // beaconSchema = database.getSchema(SCHEMA_NAME);
      beaconSchema = database.dropCreateSchema(SCHEMA_NAME);
      ProfileLoader b2l = new ProfileLoader("_profiles/FAIRDataHub.yaml");
      b2l.load(beaconSchema, true);
    }
  }

  static Request mockEntryTypeRequest(
      String entryType, Map<String, String[]> queryParams, BeaconSpec spec) {
    Request request = mock(Request.class);
    String url = TEST_URL + spec.getPath();
    when(request.attribute("specification")).thenReturn(spec.getPath());
    when(request.url()).thenReturn(url);
    Map<String, String> urlParams = Map.of(":entry_type", entryType);
    when(request.params()).thenReturn(urlParams);
    when(request.queryMap()).thenReturn(Mockito.mock(QueryParamsMap.class));
    when(request.queryMap().toMap()).thenReturn(queryParams);

    return request;
  }

  static Request mockEntryTypeRequestRegular(String entryType, Map<String, String[]> queryParams) {
    return mockEntryTypeRequest(entryType, queryParams, BEACON_V2);
  }

  static Request mockEntryRequestVp(String entryType, Map<String, String[]> queryParams) {
    return mockEntryTypeRequest(entryType, queryParams, BEACON_VP);
  }

  static BeaconRequestBody mockIndividualsPostRequestVp(String body)
      throws JsonProcessingException {
    Request request = mockEntryRequestVp(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(body, BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);

    return beaconRequest;
  }

  static BeaconRequestBody mockIndividualsPostRequestRegular(String body)
      throws JsonProcessingException {
    Request request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(body, BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);

    return beaconRequest;
  }
}
