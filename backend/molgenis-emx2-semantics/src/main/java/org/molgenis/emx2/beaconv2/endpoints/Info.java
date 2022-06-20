package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.endpoints.info.InfoResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Info {
  private String $schema = "../beaconInfoResponse.json";
  private Meta meta = new Meta("../beaconInfoResponse.json", "info");
  private InfoResponse response = new InfoResponse();
}
