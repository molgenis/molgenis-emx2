package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/** Returned schemas or Requested schemas */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Schemas {

  private String entityType;
  private String schema = "beacon-info-v2.0.0"; // TODO make static? but not allowed

  public Schemas(String entityType) {
    this.entityType = entityType;
  }
  // "beacon-dataset-v2.0.0" schema for FilteringTerms?
  public Schemas(String entityType, String schema) {
    this.entityType = entityType;
  }
}
