package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/** Use for any id-label combinations to express ontologies */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OntologyTerm {
  String id;
  String label;

  public OntologyTerm(String id, String label) {
    this.id = id;
    this.label = label;
  }

  public OntologyTerm() {
    super();
  }
}
