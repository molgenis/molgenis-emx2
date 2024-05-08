package org.molgenis.emx2.beaconv2.filter;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;

/**
 * Represents the concepts used for filtering in a query according the VP spec. <a
 * href="https://github.com/ejp-rd-vp/vp-api-specs">vp-api-specs</a>
 */
public enum FilterConceptVP {
  AGE_THIS_YEAR("ncit:C83164"),
  AGE_OF_ONSET("ncit:C124353"),
  AGE_AT_DIAG("ncit:C156420"),
  CAUSAL_GENE("edam:data_2295", "{ diseaseCausalGenes: { name: { equals: \"%s\" } } }"),
  DISEASE("ncit:C2991", "{ diseases: { diseaseCode: { ontologyTermURI: { like: \"%s\" } } } }"),
  PHENOTYPE(
      "sio:SIO_010056",
      "{ phenotypicFeatures: { featureType: { ontologyTermURI: { like: \"%s\" } } } }"),
  SEX(
      "ncit:C28421",
      "{ sex: { ontologyTermURI: { like: \"%s\" } } }",
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

  public List<String> getIso8601Durations(JsonNode result) {
    List<String> ageIso8601durations = new ArrayList<>();

    if (this == FilterConceptVP.AGE_THIS_YEAR) {
      if (result.hasNonNull("age_age_iso8601duration")) {
        ageIso8601durations.add(result.get("age_age_iso8601duration").textValue());
      }
    } else if (this == FilterConceptVP.AGE_OF_ONSET) {
      for (JsonNode disease : result.get("diseases")) {
        if (disease.hasNonNull("ageOfOnset_age_iso8601duration")) {
          ageIso8601durations.add(disease.get("ageOfOnset_age_iso8601duration").textValue());
        }
      }
    } else if (this == FilterConceptVP.AGE_AT_DIAG) {
      for (JsonNode disease : result.get("diseases")) {
        if (disease.hasNonNull("ageAtDiagnosis_age_iso8601duration")) {
          ageIso8601durations.add(disease.get("ageAtDiagnosis_age_iso8601duration").textValue());
        }
      }
    }

    return ageIso8601durations;
  }
}
