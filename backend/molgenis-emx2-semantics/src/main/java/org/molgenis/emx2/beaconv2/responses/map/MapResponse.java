package org.molgenis.emx2.beaconv2.responses.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MapResponse {
  String $schema = "../../configuration/beaconMapSchema.json";
  EndPointSets endpointSets = new EndPointSets("https://myserver.molgeniscloud.org/");
}
