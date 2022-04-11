package org.molgenis.emx2.semantics.gendecs;


public class Variant {
  private String variant;
  private String gene;
  private String disease;
  private String hpoTerm;

  public void setVariant(String variant) {
    this.variant = variant;
  }

  public String getGene() {
    return gene;
  }

  public void setGene(String gene) {
    this.gene = gene;
  }

  public String getDisease() {
    return disease;
  }

  public void setDisease(String diseases) {
    this.disease = diseases;
  }

  public void setHpoTerm(String hpoTerm) {
    this.hpoTerm = hpoTerm;
  }
}
