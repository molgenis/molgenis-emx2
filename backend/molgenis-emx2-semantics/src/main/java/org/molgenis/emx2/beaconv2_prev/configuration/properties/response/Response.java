package org.molgenis.emx2.beaconv2_prev.configuration.properties.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Response {

  String schema = "http://json-schema.org/draft-07/schema";
  String title = "Beacon Configuration";
  String description =
      "Files complaint with this schema are the configuration ones. The details returned in `service-info` are mirroring the ones in this configuration file.";
  String type = "object";

  String $ref =
      "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/configuration/beaconConfigurationSchema.json";

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class SecurityAttributes {
    String defaultGranularity = "boolean"; // "boolean", "count", "aggregated" or "record"
    String[] securityLevels =
        new String[] {
          "PUBLIC", "REGISTERED", "CONTROLLED"
        }; // any combination of "PUBLIC", "REGISTERED", "CONTROLLED"
  }

  EntryTypes entryTypes = new EntryTypes();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class EntryTypes {}
}
