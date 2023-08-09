package org.molgenis.emx2.semantics.beaconv2;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Info;
import org.molgenis.emx2.json.JsonUtil;

public class Beaconv2_InfoTest {

  @Test
  public void testInfoRootEndpoint() throws Exception {
    Info i = new Info();
    String json = JsonUtil.getWriter().writeValueAsString(i);

    // first line - note: no longer needed?
    // assertTrue(json.contains("\"$schema\" : \"../beaconInfoResponse.json\","));

    // last line (except for closing braces)
    assertTrue(json.contains("\"updateDateTime\" :"));

    // org info
    assertTrue(json.contains("organization"));
    assertTrue(json.contains("Genomics Coordination Center"));

    // beacon info
    assertTrue(json.contains("MOLGENIS EMX2 Beacon v2"));
  }
}
