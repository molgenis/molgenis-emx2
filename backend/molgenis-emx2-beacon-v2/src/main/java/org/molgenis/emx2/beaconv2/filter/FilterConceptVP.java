package org.molgenis.emx2.beaconv2.filter;

import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;

/**
 * Represents the concepts used for filtering in a query according the VP spec. <a
 * href="https://github.com/ejp-rd-vp/vp-api-specs">vp-api-specs</a>
 */
public enum FilterConceptVP {
  ID("id", "{ id: { equals:  \"%s\" } }"),
  NAME("dct:title", "{ title: { equals:  \"%s\" } }"),
  DESCRIPTION("dct:description", "{ description: { equals:  \"%s\" } }"),
  AGE_THIS_YEAR("NCIT_C83164"),
  AGE_OF_ONSET("NCIT_C124353"),
  AGE_AT_DIAG("NCIT_C156420"),
  CAUSAL_GENE("data_2295", "{ diseaseCausalGenes: { name: { equals: \"%s\" } } }"),
  DISEASE("NCIT_C2991", "{ diseases: { diseaseCode: { ontologyTermURI: { like: \"%s\" } } } }"),
  PHENOTYPE(
      "SIO_010056",
      "{ phenotypicFeatures: { featureType: { ontologyTermURI: { like: \"%s\" } } } }"),
  SEX(
      "NCIT_C28421",
      "{ sex: { ontologyTermURI: { like: \"%s\" } } }",
      List.of("NCIT_C16576", "NCIT_C20197", "NCIT_C124294", "NCIT_C17998")),
  BIOSAMPLE_TYPE(
      "NCIT_C70713",
      "{ sampleOriginType: { equals: \"%s\" } }",
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
          "OBI_0001479")),
  RESOURCE_TYPE(
      "rdf:type",
      null,
      List.of("ejprd:PatientRegistry", "ejprd:Biobank", "ejprd:Guideline", "dcat:Dataset"));

  private final String id;
  private final String graphQlQuery;
  private final List<String> permittedValues;

  FilterConceptVP(String id) {
    this.id = id;
    this.graphQlQuery = null;
    this.permittedValues = null;
  }

  FilterConceptVP(String id, String graphQlQuery) {
    this.id = id;
    this.graphQlQuery = graphQlQuery;
    this.permittedValues = null;
  }

  FilterConceptVP(String id, String graphQlQuery, List<String> permittedValues) {
    this.id = id;
    this.graphQlQuery = graphQlQuery;
    this.permittedValues = permittedValues;
  }

  public static FilterConceptVP findById(String id) {
    return Arrays.stream(values())
        .filter(concept -> concept.getId().equalsIgnoreCase(id))
        .findFirst()
        .orElseThrow(() -> new MolgenisException("Invalid concept: %s".formatted(id)));
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
