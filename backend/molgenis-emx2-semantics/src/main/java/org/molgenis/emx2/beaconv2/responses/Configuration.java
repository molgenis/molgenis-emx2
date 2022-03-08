package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.responses.configuration.ConfigurationResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Configuration {
  String $schema = "../beaconConfigurationResponse.json";
  Meta meta = new Meta();
  ConfigurationResponse response = new ConfigurationResponse();
}
