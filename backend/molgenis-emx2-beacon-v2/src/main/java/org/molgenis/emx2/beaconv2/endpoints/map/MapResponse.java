package org.molgenis.emx2.beaconv2.endpoints.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MapResponse {

  private String $schema;
  private EndPointSets endpointSets;

  public MapResponse(String serverURL) {
    this.$schema = "../../configuration/beaconMapSchema.json";
    this.endpointSets = new EndPointSets(serverURL);
  }
}
