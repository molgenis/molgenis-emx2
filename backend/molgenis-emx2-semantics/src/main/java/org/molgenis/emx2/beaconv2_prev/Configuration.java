package org.molgenis.emx2.beaconv2_prev;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Configuration {

  String $schema = "../beaconConfigurationSchema.json";

  /*
  Attributes from beaconConfigurationResponse.json
   */
  String type = "object";
  String description =
      "Information about the Beacon. Aimed to Beacon clients like web pages or Beacon networks.";
  Properties properties = new Properties();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class Properties {
    Meta meta = new Meta();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Meta {
      String description =
          "Information about the response that could be relevant for the Beacon client in order to interpret the results.";
      String $ref =
          "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/responses/sections/beaconInformationalResponseMeta.json";
    }

    Response response = new Response();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Response {
      String description = "Returning the Beacon configuration.";
      String $ref =
          "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/configuration/beaconConfigurationSchema.json";
    }
  }

  String[] required = new String[] {"meta", "response"};
  boolean additionalProperties = true;

  /*
  Attributes from beaconConfiguration-example.json
  todo
   */

  MaturityAttributes maturityAttributes = new MaturityAttributes();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class MaturityAttributes {
    String productionStatus = "DEV"; // DEV, TEST or PROD
  }

  SecurityAttributes securityAttributes = new SecurityAttributes();

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
