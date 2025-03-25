package org.molgenis.emx2.beaconv2.entrytypes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.beaconv2.BeaconSpec.BEACON_V2;
import static org.molgenis.emx2.beaconv2.BeaconSpec.BEACON_VP;
import static org.molgenis.emx2.datamodels.DataModels.Regular.RD3_V2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.BeaconSpec;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.sql.TestDatabaseFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeaconModelEndPointTest {

  public static final String SCHEMA_NAME = "rd3_beacon";
  public static final String TEST_URL = "http://localhost:8080/" + SCHEMA_NAME + "/api/";

  protected static Database database;
  protected static Schema beaconSchema;

  @BeforeAll
  public void setup() {
    if (database == null) {
      database = TestDatabaseFactory.getTestDatabase();
      // beaconSchema = database.getSchema(SCHEMA_NAME);
      beaconSchema = database.dropCreateSchema(SCHEMA_NAME);
      RD3_V2.getImportTask(beaconSchema, true).run();
    }
  }

  static Context mockEntryTypeRequest(
      String entryType, Map<String, List<String>> queryParams, BeaconSpec spec) {
    Context request = mock(Context.class);
    String url = TEST_URL + spec.getPath();
    when(request.attribute("specification")).thenReturn(spec.getPath());
    when(request.url()).thenReturn(url);
    Map<String, String> urlParams = Map.of("entry_type", entryType);
    when(request.pathParamMap()).thenReturn(urlParams);
    when(request.queryParamMap()).thenReturn(queryParams);

    return request;
  }

  static Context mockEntryTypeRequestRegular(
      String entryType, Map<String, List<String>> queryParams) {
    return mockEntryTypeRequest(entryType, queryParams, BEACON_V2);
  }

  static Context mockEntryRequestVp(String entryType, Map<String, List<String>> queryParams) {
    return mockEntryTypeRequest(entryType, queryParams, BEACON_VP);
  }

  static BeaconRequestBody mockIndividualsPostRequestVp(String body)
      throws JsonProcessingException {
    Context request = mockEntryRequestVp(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(body, BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);

    return beaconRequest;
  }

  static BeaconRequestBody mockIndividualsPostRequestRegular(String body)
      throws JsonProcessingException {
    Context request = mockEntryTypeRequestRegular(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    ObjectMapper mapper = new ObjectMapper();
    BeaconRequestBody beaconRequest = mapper.readValue(body, BeaconRequestBody.class);
    beaconRequest.addRequestParameters(request);

    return beaconRequest;
  }
}
