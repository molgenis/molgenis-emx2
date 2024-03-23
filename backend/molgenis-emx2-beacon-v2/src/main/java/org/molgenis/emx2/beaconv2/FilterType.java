package org.molgenis.emx2.beaconv2;

public enum FilterType {
  ONTOLOGY(null),
  ALPHANUMERICAL(null),
  NUMERICAL(null),
  UNDEFINED(null);

  private Concept concept;

  FilterType(Concept concept) {
    this.concept = concept;
  }

  public void setConcept(Concept concept) {
    this.concept = concept;
  }
}
