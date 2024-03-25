package org.molgenis.emx2.beaconv2;

import java.util.List;
import org.molgenis.emx2.MolgenisException;

public enum Concept {
  DISEASE(
      "NCIT_C2991", "{ diseases: { diseaseCode: { ontologyTermURI: { like: \"%s\" } } } }", null),
  PHENOTYPE(
      "SIO_010056",
      "{ phenotypicFeatures: { featureType: { ontologyTermURI: { like: \"%s\" } } } }",
      null),
  SEX(
      "NCIT_C28421",
      "{ sex: { ontologyTermURI: { like: \"%s\" } } }",
      List.of("NCIT_C16576", "NCIT_C20197", "NCIT_C124294", "NCIT_C17998")),
  CAUSAL_GENE("data_2295", "{ diseaseCausalGenes: { name: { equals: \"%s\" } } }", null),
  AGE_THIS_YEAR("NCIT_C83164", null, null),
  AGE_OF_ONSET("NCIT_C124353", null, null),
  AGE_AT_DIAG("NCIT_C156420", null, null);

  private final String id;
  private final String graphQlQuery;
  private final List<String> permittedValues;

  Concept(String id, String graphQlQuery, List<String> permittedValues) {
    this.id = id;
    this.graphQlQuery = graphQlQuery;
    this.permittedValues = permittedValues;
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

  public List<String> getPermittedValues() {
    return permittedValues;
  }
}
