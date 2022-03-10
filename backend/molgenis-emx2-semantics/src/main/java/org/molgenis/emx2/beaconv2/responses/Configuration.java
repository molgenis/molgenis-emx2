package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.CommonMeta;
import org.molgenis.emx2.beaconv2.responses.configuration.ConfigurationResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Configuration {
  CommonMeta meta = new CommonMeta("../beaconConfigurationResponse.json", "configuration");
  ConfigurationResponse response = new ConfigurationResponse();
}
