package org.molgenis.emx2.beaconv2;

public enum FilterType {
  ONTOLOGY(
      null,
      """
          { diseases: { diseaseCode: { ontologyTermURI: { like: "%1$s" } } } },
          { phenotypicFeatures: { featureType: { ontologyTermURI: {like: "%1$s" } } } }
          """),
  ALPHANUMERICAL(null, null),
  NUMERICAL(null, null),
  UNDEFINED(null, null);

  private Concept concept;

  private String graphQlQuery;

  FilterType(Concept concept, String graphQlQuery) {
    this.concept = concept;
    this.graphQlQuery = graphQlQuery;
  }

  public String getGraphQlFilter() {
    return this.graphQlQuery;
  }

  public void setConcept(Concept concept) {
    this.concept = concept;
    if (concept.getGraphQlQuery() != null) {
      this.graphQlQuery = concept.getGraphQlQuery();
    }
  }
}
