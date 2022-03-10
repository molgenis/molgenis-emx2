package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.CommonMeta;
import org.molgenis.emx2.beaconv2.responses.info.InfoResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Info {
  String $schema = "../beaconInfoResponse.json";
  CommonMeta meta = new CommonMeta("../beaconInfoResponse.json", "info");
  InfoResponse response = new InfoResponse();
}
