package org.molgenis.emx2.beaconv2.common.misc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HandoverType {

  // Handover type, as an Ontology_term object with CURIE syntax for the\n`id` value.
  private String id;

  // This would be the \"preferred Label\" in the case of an ontology term.
  private String label;

  public HandoverType() {
    this.id = "EMX2:000001";
    this.label = "MOLGENIS EMX2 Beacon v2 handover";
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }
}
