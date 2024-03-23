package org.molgenis.emx2.beaconv2;

import org.molgenis.emx2.MolgenisException;

public enum Concept {
  DISEASE("NCIT_C2991", "{ diseases: { diseaseCode: { ontologyTermURI: { like: \"%s\" } } } }"),
  PHENOTYPE(
      "SIO_010056",
      "{ phenotypicFeatures: { featureType: { ontologyTermURI: { like: \"%s\" } } } }"),
  SEX("NCIT_C28421", "{ sex: { ontologyTermURI: { like: \"%s\" } } }"),
  CAUSAL_GENE("data_2295", "{ diseaseCausalGenes: { name: { equals: \"%s\" } } }"),
  AGE_THIS_YEAR("NCIT_C83164", null),
  AGE_OF_ONSET("NCIT_C124353", null),
  AGE_AT_DIAG("NCIT_C156420", null);

  private final String id;
  private final String graphQlQuery;

  Concept(String id, String graphQlQuery) {
    this.id = id;
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

  public String getGraphQlQuery() {
    return graphQlQuery;
  }
}
