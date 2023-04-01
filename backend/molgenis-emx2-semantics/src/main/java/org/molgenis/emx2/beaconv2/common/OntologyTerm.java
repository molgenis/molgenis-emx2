package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Use for any id-label combinations to express ontologies */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OntologyTerm {
  private String id;
  private String label;

  // no getter because JSON will pick it up again, instead public...
  @JsonIgnore public String URI;

  public OntologyTerm(String id, String label, String URI) {
    this.id = id;
    this.label = label;
    this.URI = URI;
  }

  public OntologyTerm() {}

  public void setId(String id) {
    this.id = id;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public void setURI(String URI) {
    this.URI = URI;
  }
}
