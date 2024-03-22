package org.molgenis.emx2.beaconv2;

import org.molgenis.emx2.MolgenisException;

public enum Concept {
  DISEASE(
      "NCIT_C2991",
      //      FilterType.ONTOLOGY,
      "{ diseases: { diseaseCode: { ontologyTermURI: { like: \"%s\" } } } }"),
  PHENOTYPE(
      "SIO_010056",
      //      FilterType.ONTOLOGY,
      "{ phenotypicFeatures: { featureType: { ontologyTermURI: { like: \"$s\" } } } }"),
  SEX(
      "NCIT_C28421",
      //      FilterType.ALPHANUMERICAL,
      "{ sex: { ontologyTermURI: { like: \"%s\" } } }"),
  CAUSAL_GENE(
      "data_2295",
      //      FilterType.ALPHANUMERICAL,
      " { diseaseCausalGenes: { name: { equals: \"%s\" } } }"),
  AGE_THIS_YEAR(
      "NCIT_C83164",
      //      FilterType.NUMERICAL,
      null),
  AGE_OF_ONSET(
      "NCIT_C124353",
      //      FilterType.NUMERICAL,
      null),
  AGE_AT_DIAG(
      "NCIT_C156420",
      //      FilterType.NUMERICAL,
      null);

  private final String id;
  //  private final FilterType filterType;
  private final String graphQlQuery;

  Concept(
      String id,
      //          FilterType filterType,
      String graphQlQuery) {
    this.id = id;
    //    this.filterType = filterType;
    this.graphQlQuery = graphQlQuery;
  }

  public static Concept findById(String id) {
    Concept result = null;
    for (Concept concept : values()) {
      if (concept.getId().equalsIgnoreCase(id)) {
        result = concept;
        break;
      }
    }
    if (result == null) throw new MolgenisException("Invalid concept: %s".formatted(id));

    return result;
  }

  public String getId() {
    return id;
  }

  //  public FilterType getFilterType() {
  //    return filterType;
  //  }

  public String getGraphQlQuery() {
    return graphQlQuery;
  }
}
