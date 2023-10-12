package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.endpoints.configuration.ConfigurationResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Configuration {
  private Meta meta = new Meta("../beaconConfigurationResponse.json", "configuration");
  private ConfigurationResponse response = new ConfigurationResponse();
}
