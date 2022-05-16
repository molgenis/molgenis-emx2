package org.molgenis.emx2.beaconv2.responses.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MapResponse {

  String $schema;
  EndPointSets endpointSets;

  public MapResponse(String serverURL) {
    this.$schema = "../../configuration/beaconMapSchema.json";
    this.endpointSets = new EndPointSets(serverURL);
  }
}
