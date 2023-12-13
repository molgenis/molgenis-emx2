package org.molgenis.emx2.beaconv2.common.misc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SchemasPerEntity {
  private String entityType;
  private String schema;

  public SchemasPerEntity(String entityType, String schema) {
    this.entityType = entityType;
    this.schema = schema;
  }
}
