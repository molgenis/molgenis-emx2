package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VariantLevelData {

  private ClinicalInterpretations[] clinicalInterpretations;

  public ClinicalInterpretations[] getClinicalInterpretations() {
    return clinicalInterpretations;
  }

  public void setClinicalInterpretations(ClinicalInterpretations[] clinicalInterpretations) {
    this.clinicalInterpretations = clinicalInterpretations;
  }

  public VariantLevelData(ClinicalInterpretations[] clinicalInterpretations) {
    this.clinicalInterpretations = clinicalInterpretations;
  }
}
