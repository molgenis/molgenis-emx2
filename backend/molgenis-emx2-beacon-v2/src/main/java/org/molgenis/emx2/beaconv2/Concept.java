package org.molgenis.emx2.beaconv2;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
  AGE_AT_DIAG("NCIT_C156420", null, null),
  BIOSPECIMIN_TYPE(
      "NCIT_C70713",
      null,
      List.of(
          "OBI_0000655",
          "OBI_0002512",
          "OBIB_0000036",
          "CL_2000001",
          "OBI_0100016",
          "OBI_0100017",
          "UBERON_0007795",
          "OBI_0002502",
          "OBI_0002507",
          "OBI_0002503",
          "OBI_0000651",
          "OBI_0002599",
          "OBI_2000009",
          "OBI_1200000",
          "OBI_0000922",
          "OBI_0001472",
          "OBI_0001051",
          "OBI_0000880",
          "OBI_0001479"));

  private final String id;
  private final String graphQlQuery;
  private final List<String> permittedValues;

  Concept(String id, String graphQlQuery, List<String> permittedValues) {
    this.id = id;
    this.graphQlQuery = graphQlQuery;
    this.permittedValues = permittedValues;
  }

  public static Concept findById(String id) {
    Optional<Concept> result = Arrays.stream(values()).findFirst();
    if (result.isEmpty()) throw new MolgenisException("Invalid concept: %s".formatted(id));

    return result.get();
  }

  private String getId() {
    return id;
  }

  public String getGraphQlQuery() {
    return graphQlQuery;
  }

  public boolean isPermittedValue(String[] values) {
    if (permittedValues != null) {
      return Arrays.stream(values).allMatch(permittedValues::contains);
    }
    return true;
  }
}
