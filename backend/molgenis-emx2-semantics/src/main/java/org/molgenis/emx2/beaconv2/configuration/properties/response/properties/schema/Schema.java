package org.molgenis.emx2.beaconv2.configuration.properties.response.properties.schema;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Schema {
  String $ref = "../common/beaconCommonComponents.json#/definitions/$schema";
}
