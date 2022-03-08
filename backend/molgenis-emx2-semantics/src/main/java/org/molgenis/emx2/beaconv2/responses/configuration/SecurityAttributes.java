package org.molgenis.emx2.beaconv2.responses.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SecurityAttributes {
  String defaultGranularity = "record";
  Map<String, String> securityLevels =
      new HashMap<String, String>() {
        {
          put("0", "PUBLIC");
          put("1", "CONTROLLED");
        }
      };
}
