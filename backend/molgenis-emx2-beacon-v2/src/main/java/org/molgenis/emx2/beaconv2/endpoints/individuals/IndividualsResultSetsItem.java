package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.AgeAndAgeGroup;
import org.molgenis.emx2.beaconv2.endpoints.genomicvariants.CaseLevelData;
import org.molgenis.emx2.semantics.OntologyTerm;

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
  private CaseLevelData[] hasGenomicVariations;

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

  public String getId() {
    return id;
  }

  public OntologyTerm getSex() {
    return sex;
  }

  public AgeAndAgeGroup getAge() {
    return age;
  }

  public OntologyTerm[] getDiseaseCausalGenes() {
    return diseaseCausalGenes;
  }

  public OntologyTerm getEthnicity() {
    return ethnicity;
  }

  public OntologyTerm getGeographicOrigin() {
    return geographicOrigin;
  }

  public Diseases[] getDiseases() {
    return diseases;
  }

  public Measures[] getMeasures() {
    return measures;
  }

  public PhenotypicFeatures[] getPhenotypicFeatures() {
    return phenotypicFeatures;
  }

  public CaseLevelData[] getHasGenomicVariations() {
    return hasGenomicVariations;
  }

  public void setHasGenomicVariations(CaseLevelData[] hasGenomicVariations) {
    this.hasGenomicVariations = hasGenomicVariations;
  }
}
