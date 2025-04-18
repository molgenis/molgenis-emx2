package org.molgenis.emx2.beaconv2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beaconv2.endpoints.Info;
import org.molgenis.emx2.beaconv2.entrytypes.BeaconModelEndPointTest;

@Disabled
public class Beaconv2_InfoTest extends BeaconModelEndPointTest {

  @Test
  public void testInfoRootEndpoint() {
    Info info = new Info(database);
    JsonNode response = info.getResponse(beaconSchema);

    assertEquals("https://molgenis.org/", response.get("response").get("welcomeUrl").asText());
    assertEquals("MOLGENIS EMX2 Beacon v2", response.get("response").get("name").asText());
  }
}
