package org.molgenis.emx2.beaconv2_prev.configuration.properties.response.properties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2_prev.configuration.properties.response.properties.schema.Schema;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Properties {
  Schema schema = new Schema();
}
