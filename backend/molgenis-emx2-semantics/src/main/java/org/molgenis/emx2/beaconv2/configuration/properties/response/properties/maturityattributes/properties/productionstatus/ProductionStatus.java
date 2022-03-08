package org.molgenis.emx2.beaconv2.configuration.properties.response.properties.maturityattributes.properties.productionstatus;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ProductionStatus {
  String description =
      "`DEV`= 'Service potentially unstable, not real data', which availability and data should not be used in production setups. `TEST`= 'Service stable, not real data'. 'PROD'= 'Service stable, actual data'.";
  String type = "string";
  String[] _enum =
      new String[] {
        "DEV", "TEST", "PROD"
      }; // FIXME: this is actually called 'enum' which is a reserved word
}
