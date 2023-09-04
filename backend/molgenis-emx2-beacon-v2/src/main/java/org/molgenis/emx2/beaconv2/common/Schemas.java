package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/** Returned schemas or Requested schemas */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Schemas {

  private String entityType;
  private String schema;

  public Schemas(String entityType) {
    this.schema = "beacon-info-v2.0.0";
    this.entityType = entityType;
  }

  public Schemas(String entityType, String schema) {
    this.entityType = entityType;
  }

  public String getEntityType() {
    return entityType;
  }

  public String getSchema() {
    return schema;
  }
}
