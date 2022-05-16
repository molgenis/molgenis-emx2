package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.CommonMeta;
import org.molgenis.emx2.beaconv2.responses.map.MapResponse;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Map {

  CommonMeta meta;
  MapResponse response;

  public Map(Request request) {
    this.meta = new CommonMeta("../beaconMapResponse.json", "map");
    String serverURL = request.url().replace("api/beacon/map", "");
    this.response = new MapResponse(serverURL);
  }
}
