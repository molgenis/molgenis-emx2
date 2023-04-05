package org.molgenis.emx2.semantics.beaconv2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Info;
import org.molgenis.emx2.json.JsonUtil;

public class Beaconv2_InfoTest {

  @Test
  public void testInfoRootEndpoint() throws Exception {
    Info i = new Info();
    String json = JsonUtil.getWriter().writeValueAsString(i);

    // first line
    assertTrue(json.contains("\"$schema\" : \"../beaconInfoResponse.json\","));

    // last line (except for closing braces)
    assertTrue(json.contains("\"updateDateTime\" : \"2022-01-01\""));

    // org info
    assertTrue(
        json.contains(
            "    \"organization\" : {\n"
                + "      \"id\" : \"gcc\",\n"
                + "      \"name\" : \"Genomics Coordination Center\","));

    // beacon info
    assertTrue(
        json.contains(
            "  \"response\" : {\n"
                + "    \"id\" : \"molgenis\",\n"
                + "    \"name\" : \"MOLGENIS Beacon v2\","));

    assertEquals(1181, json.length());
  }
}
