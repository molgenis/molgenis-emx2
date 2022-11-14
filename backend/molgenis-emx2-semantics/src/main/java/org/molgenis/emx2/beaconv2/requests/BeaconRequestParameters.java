package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconRequestParameters {
  String $ref = "./requestParameters.json";
  String description = "Parameters used for the entry type specific query";
}
