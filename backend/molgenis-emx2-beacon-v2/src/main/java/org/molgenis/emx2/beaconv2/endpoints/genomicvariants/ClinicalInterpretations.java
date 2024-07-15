package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.endpoints.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ClinicalInterpretations {

  private OntologyTerm category;
  private OntologyTerm clinicalRelevance;
  private String conditionId;
  private OntologyTerm effect;

  public OntologyTerm getCategory() {
    return category;
  }

  public OntologyTerm getClinicalRelevance() {
    return clinicalRelevance;
  }

  public String getConditionId() {
    return conditionId;
  }

  public OntologyTerm getEffect() {
    return effect;
  }
}
