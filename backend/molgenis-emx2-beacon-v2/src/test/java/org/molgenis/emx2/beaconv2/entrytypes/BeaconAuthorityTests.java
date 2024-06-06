package org.molgenis.emx2.beaconv2.entrytypes;

import static org.molgenis.emx2.Constants.ANONYMOUS;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.QueryEntryType;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import spark.Request;

public class BeaconAuthorityTests extends BeaconModelEndPointTest {

  @Test
  public void testRecordQueryAsAggregateUser_noRecords() {
    beaconSchema.removeMember(ANONYMOUS);
    database.setActiveUser("AGGREGATE_TEST_USER");
    Request request = mockEntryTypeRequest(EntryType.INDIVIDUALS.getId(), new HashMap<>());
    BeaconRequestBody requestBody = new BeaconRequestBody(request);

    QueryEntryType queryEntryType = new QueryEntryType(requestBody);
    JsonNode json = queryEntryType.query(database);
    String test = "";
  }
}
