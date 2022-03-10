package org.molgenis.emx2.beaconv2.responses.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReturnedSchemas {
  String entityType = "info";
  String schema = "beacon-info-v2.0.0-draft.4";

  public ReturnedSchemas(String entityType) {
    this.entityType = entityType;
  }
}
