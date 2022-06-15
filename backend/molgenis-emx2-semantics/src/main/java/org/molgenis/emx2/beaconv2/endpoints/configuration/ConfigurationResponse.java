package org.molgenis.emx2.beaconv2.endpoints.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.EntryTypes;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ConfigurationResponse {
  String $schema =
      "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/configuration/beaconConfigurationSchema.json";
  MaturityAttributes maturityAttributes = new MaturityAttributes();
  SecurityAttributes securityAttributes = new SecurityAttributes();
  EntryTypes entryTypes = new EntryTypes();
}
