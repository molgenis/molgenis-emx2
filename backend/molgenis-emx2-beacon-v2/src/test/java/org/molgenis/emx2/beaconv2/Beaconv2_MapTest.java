package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Map;
import org.molgenis.emx2.json.JsonUtil;
import spark.Request;

public class Beaconv2_MapTest {

  @Test
  public void testMap() throws Exception {
    Request request = mock(Request.class);
    when(request.url()).thenReturn("http://localhost:8080/myEmx2Schema/api/beacon/map");
    Map m = new Map(request);
    String json = JsonUtil.getWriter().writeValueAsString(m);

    // first line
    assertTrue(json.contains("  \"meta\" : {\n" + "    \"beaconId\" : \"org.molgenis.beaconv2\","));

    // return map schema
    assertTrue(
        json.contains(
            "  \"response\" : {\n"
                + "    \"$schema\" : \"../../configuration/beaconMapSchema.json\","));

    // check url construction for 'analyses'
    assertTrue(
        json.contains("\"rootUrl\" : \"http://localhost:8080/myEmx2Schema/api/beacon/analyses\","));
    assertTrue(
        json.contains(
            "\"singleEntryUrl\" : \"http://localhost:8080/myEmx2Schema/api/beacon/analyses/{id}\","));
    assertTrue(
        json.contains(
            "\"filteringTermsUrl\" : \"http://localhost:8080/myEmx2Schema/api/beacon/analyses/{id}/filtering_terms\""));
  }
}
