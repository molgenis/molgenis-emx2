package org.molgenis.emx2.beaconv2.entrytypes;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.datamodels.ProfileLoader;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import spark.QueryParamsMap;
import spark.Request;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BeaconModelEndPointTest {

  static Database database;
  static Schema beaconSchema;

  @BeforeAll
  public static void setup() {
    if (database == null) {
      database = TestDatabaseFactory.getTestDatabase();
      // beaconSchema = database.getSchema("fairdatahub");
      beaconSchema = database.dropCreateSchema("fairdatahub");
      ProfileLoader b2l = new ProfileLoader("_profiles/FAIRDataHub.yaml");
      b2l.load(beaconSchema, true);
    }
  }

  static Request mockEntryTypeRequest(String entryType, Map<String, String[]> queryParams) {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/api/beacon");
    when(request.attribute("specification")).thenReturn("beacon");
    Map<String, String> urlParams = Map.of(":entry_type", entryType);
    when(request.params()).thenReturn(urlParams);
    when(request.queryMap()).thenReturn(Mockito.mock(QueryParamsMap.class));
    when(request.queryMap().toMap()).thenReturn(queryParams);

    return request;
  }
}
