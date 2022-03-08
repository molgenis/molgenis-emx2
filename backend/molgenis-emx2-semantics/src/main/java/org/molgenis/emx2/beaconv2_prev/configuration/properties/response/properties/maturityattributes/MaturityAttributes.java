package org.molgenis.emx2.beaconv2_prev.configuration.properties.response.properties.maturityattributes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MaturityAttributes {
  String description = "Declares the level of maturity of the Beacon instance.";
  String type = "object";
  String productionStatus = "DEV"; // DEV, TEST or PROD
}
