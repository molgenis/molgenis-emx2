package org.molgenis.emx2.beaconv2.filter;

import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;

/**
 * Represents the concepts used for filtering in a query according the VP spec. <a
 * href="https://github.com/ejp-rd-vp/vp-api-specs">vp-api-specs</a>
 */
public enum FilterConceptVP {
  AGE_THIS_YEAR("ncit:C83164", "{ yearOfBirth: { between: [%s, %s] } }"),
  AGE_OF_ONSET(
      "ncit:C124353", "{ processes: { diseases: { ageOfOnset: { between: [%s, %s] } } } }"),
  AGE_AT_DIAG(
      "ncit:C156420", "{ processes: { diseases: { ageAtDiagnosis: { between: [%s, %s] } }  } }"),
  CAUSAL_GENE(
      "edam:data_2295", "{ processes: { molecularDiagnosisGene: { name: { equals: \"%s\" } } } }"),
  DISEASE(
      "ncit:C2991",
      "{ processes: { diseases: { disease: { ontologyTermURI: { like: \"%s\" } } } } }"),
  PHENOTYPE(
      "sio:SIO_010056",
      "{ processes: { phenotypes: { type: { ontologyTermURI: { like: \"%s\" } } } } }"),
  SEX(
      "ncit:C28421",
      "{ genderAtBirth: { ontologyTermURI: { like: \"%s\" } } }",
      List.of("ncit:C16576", "ncit:C20197", "ncit:C124294", "ncit:C17998")),
  BIOSAMPLE_TYPE(
      "ncit:C70713",
      "{ sampleOriginType: { equals: \"%s\" } }",
      List.of(
          "obi:0000655",
          "obi:0002512",
          "obi:0000036",
          "cl_2000001",
          "obi:0100016",
          "obi:0100017",
          "uberon_0007795",
          "obi:0002502",
          "obi:0002507",
          "obi:0002503",
          "obi:0000651",
          "obi:0002599",
          "obi:2000009",
          "obi:1200000",
          "obi:0000922",
          "obi:0001472",
          "obi:0001051",
          "obi:0000880",
          "obi:0001479")),
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

  public static boolean hasId(String id) {
    return Arrays.stream(values()).anyMatch(concept -> concept.getId().equalsIgnoreCase(id));
  }

  public String getId() {
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
