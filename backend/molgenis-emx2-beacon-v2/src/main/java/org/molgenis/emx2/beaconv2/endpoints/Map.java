package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.endpoints.map.MapResponse;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Map {

  private Meta meta;
  private MapResponse response;

  public Map(Request request) {
    this.meta = new Meta("../beaconMapResponse.json", "map");
    String serverURL = request.url().replace("api/beacon/map", "");
    this.response = new MapResponse(serverURL);
  }
}
