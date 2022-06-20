package org.molgenis.emx2.beaconv2.endpoints.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.EntryTypes;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ConfigurationResponse {
  private String $schema =
      "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/configuration/beaconConfigurationSchema.json";
  private MaturityAttributes maturityAttributes = new MaturityAttributes();
  private SecurityAttributes securityAttributes = new SecurityAttributes();
  private EntryTypes entryTypes = new EntryTypes();
}
