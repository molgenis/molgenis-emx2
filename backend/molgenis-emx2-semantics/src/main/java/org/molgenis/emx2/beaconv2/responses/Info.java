package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.responses.info.InfoResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Info {
  String $schema = "../beaconInfoResponse.json";
  Meta meta = new Meta();
  InfoResponse response = new InfoResponse();
}
