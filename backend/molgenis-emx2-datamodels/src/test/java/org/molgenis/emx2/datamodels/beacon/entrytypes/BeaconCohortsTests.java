package org.molgenis.emx2.datamodels.beacon.entrytypes;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.datamodels.beacon.BeaconTestUtil.mockEntryTypeRequestRegular;
import static org.molgenis.emx2.sql.SqlDatabase.ADMIN_USER;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.datamodels.TestLoaders;
import org.molgenis.emx2.graphql.GraphqlSession;

@Disabled
public class BeaconCohortsTests extends TestLoaders {

  @Test
  public void testCohorts_NoParams() {
    Context request = mockEntryTypeRequestRegular(EntryType.COHORTS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode cohorts = queryEntryType.query(new GraphqlSession(ADMIN_USER));
    assertTrue(cohorts.get("response").get("collections").size() >= 3);
  }

  @Test
  public void testCohorts_NoHits() throws Exception {
    Context request =
        mockEntryTypeRequestRegular(
            EntryType.COHORTS.getId(), Map.of("id", List.of(new String[] {"Cohort0003"})));
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode cohorts = queryEntryType.query(new GraphqlSession(ADMIN_USER));

    //    String json = JsonUtil.getWriter().writeValueAsString(cohorts);
    //    assertTrue(json.contains("\"collections\" : [ ]"));
  }
}
