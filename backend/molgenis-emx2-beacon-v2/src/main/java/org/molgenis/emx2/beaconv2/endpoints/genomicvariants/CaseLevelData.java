package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CaseLevelData {

  private ClinicalInterpretations[] clinicalInterpretations;
  private String individualId;

  public ClinicalInterpretations[] getClinicalInterpretations() {
    return clinicalInterpretations;
  }

  public void setClinicalInterpretations(ClinicalInterpretations[] clinicalInterpretations) {
    this.clinicalInterpretations = clinicalInterpretations;
  }

  public String getIndividualId() {
    return individualId;
  }

  public void setIndividualId(String individualId) {
    this.individualId = individualId;
  }

  public CaseLevelData() {
    super();
  }
}
