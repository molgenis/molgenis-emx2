package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.AgeAndAgeGroup;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResultSetsItem {

  private String id;
  private OntologyTerm sex;
  private AgeAndAgeGroup age;
  private OntologyTerm[] diseaseCausalGenes;
  private OntologyTerm ethnicity;
  private OntologyTerm geographicOrigin;
  private Diseases[] diseases;
  private Measures[] measures;
  private PhenotypicFeatures[] phenotypicFeatures;

  public void setPhenotypicFeatures(PhenotypicFeatures[] phenotypicFeatures) {
    this.phenotypicFeatures = phenotypicFeatures;
  }

  public void setDiseaseCausalGenes(OntologyTerm[] diseaseCausalGenes) {
    this.diseaseCausalGenes = diseaseCausalGenes;
  }

  public void setAge(AgeAndAgeGroup age) {
    this.age = age;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setSex(OntologyTerm sex) {
    this.sex = sex;
  }

  public void setEthnicity(OntologyTerm ethnicity) {
    this.ethnicity = ethnicity;
  }

  public void setGeographicOrigin(OntologyTerm geographicOrigin) {
    this.geographicOrigin = geographicOrigin;
  }

  public void setDiseases(Diseases[] diseases) {
    this.diseases = diseases;
  }

  public void setMeasures(Measures[] measures) {
    this.measures = measures;
  }
}
