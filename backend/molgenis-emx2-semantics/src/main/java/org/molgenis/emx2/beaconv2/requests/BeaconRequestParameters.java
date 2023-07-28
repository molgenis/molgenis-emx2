package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconRequestParameters {

  String $ref;
  String description;

  public BeaconRequestParameters(String $ref, String description) {
    this.$ref = $ref;
    this.description = description;
  }

  public BeaconRequestParameters() {
    this.$ref = "./requestParameters.json";
    this.description = "Parameters used for the entry type specific query";
  }

  public void empty() {
    this.$ref = null;
    this.description = null;
  }
}
