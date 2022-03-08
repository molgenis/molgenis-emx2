package org.molgenis.emx2.beaconv2_prev.configuration.properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2_prev.configuration.properties.meta.Meta;
import org.molgenis.emx2.beaconv2_prev.configuration.properties.response.Response;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Properties {
  Meta meta = new Meta();
  Response response = new Response();
}
