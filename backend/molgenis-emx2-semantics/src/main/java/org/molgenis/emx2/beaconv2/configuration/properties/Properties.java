package org.molgenis.emx2.beaconv2.configuration.properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.configuration.properties.meta.Meta;
import org.molgenis.emx2.beaconv2.configuration.properties.response.Response;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Properties {
  Meta meta = new Meta();
  Response response = new Response();
}
