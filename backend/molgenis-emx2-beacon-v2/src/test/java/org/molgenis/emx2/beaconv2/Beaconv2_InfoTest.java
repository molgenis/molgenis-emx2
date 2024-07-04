package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Info;
import org.molgenis.emx2.beaconv2.entrytypes.BeaconModelEndPointTest;

public class Beaconv2_InfoTest extends BeaconModelEndPointTest {

  @Test
  public void testInfoRootEndpoint() {
    Info info = new Info(beaconSchema);
    JsonNode response = info.getResponse();

    assertEquals("UMCG", response.get("response").get("organization").get("name").asText());
    assertEquals("MOLGENIS EMX2 Beacon v2", response.get("response").get("name").asText());
  }
}
