package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.endpoints.info.InfoResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Info {
  String $schema = "../beaconInfoResponse.json";
  Meta meta = new Meta("../beaconInfoResponse.json", "info");
  InfoResponse response = new InfoResponse();
}
