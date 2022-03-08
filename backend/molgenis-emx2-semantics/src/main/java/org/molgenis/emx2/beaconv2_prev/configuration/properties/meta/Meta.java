package org.molgenis.emx2.beaconv2_prev.configuration.properties.meta;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Meta {
  String description =
      "Information about the response that could be relevant for the Beacon client in order to interpret the results.";
  String $ref =
      "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/responses/sections/beaconInformationalResponseMeta.json";
}
