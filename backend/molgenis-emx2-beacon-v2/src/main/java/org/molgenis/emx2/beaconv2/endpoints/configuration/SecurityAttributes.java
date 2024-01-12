package org.molgenis.emx2.beaconv2.endpoints.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SecurityAttributes {
  private String defaultGranularity = "record";
  private String[] securityLevels =
      new String[] {
        "PUBLIC", "REGISTERED", "CONTROLLED"
      }; // any combination of "PUBLIC", "REGISTERED", "CONTROLLED"
}
