package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Arrays;
import org.molgenis.emx2.beaconv2.responses.configuration.ReturnedSchemas;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Meta {
  String beaconId = "org.molgenis.beaconv2";
  String apiVersion = "v2.0.0-draft.4";
  ReturnedSchemas[] returnedSchemas =
      Arrays.asList(new ReturnedSchemas()).toArray(ReturnedSchemas[]::new);
}
