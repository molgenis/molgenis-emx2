package org.molgenis.emx2.beaconv2_prev.configuration.properties.response.properties.securityattributes.properties.defaultgranularity;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DefaultGranularity {
  String description =
      "Default granularity. Some responses could return higher detail, but this would be the granularity by default.";
  String $ref = "../common/beaconCommonComponents.json#/definitions/Granularity";
}
