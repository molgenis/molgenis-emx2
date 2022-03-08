package org.molgenis.emx2.beaconv2.configuration.properties.response.properties.securityattributes.properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.configuration.properties.response.properties.securityattributes.properties.defaultgranularity.DefaultGranularity;
import org.molgenis.emx2.beaconv2.configuration.properties.response.properties.securityattributes.properties.securitylevels.SecurityLevels;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Properties {
  DefaultGranularity defaultGranularity = new DefaultGranularity();
  SecurityLevels securityLevels = new SecurityLevels();
}
