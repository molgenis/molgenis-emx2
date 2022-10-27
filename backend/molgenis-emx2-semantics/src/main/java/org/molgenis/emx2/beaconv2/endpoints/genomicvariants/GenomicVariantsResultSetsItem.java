package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariantsResultSetsItem {

  private String variantInternalId;
  private String variantType;
  private String referenceBases;
  private String alternateBases;
  private Position position;
  private String geneId;
  private VariantLevelData variantLevelData;
  private CaseLevelData[] caseLevelData;

  public void setVariantInternalId(String variantInternalId) {
    this.variantInternalId = variantInternalId;
  }

  public void setVariantType(String variantType) {
    this.variantType = variantType;
  }

  public void setReferenceBases(String referenceBases) {
    this.referenceBases = referenceBases;
  }

  public void setAlternateBases(String alternateBases) {
    this.alternateBases = alternateBases;
  }

  public void setPosition(Position position) {
    this.position = position;
  }

  public void setGeneId(String geneId) {
    this.geneId = geneId;
  }

  public VariantLevelData getVariantLevelData() {
    return variantLevelData;
  }

  public void setVariantLevelData(VariantLevelData variantLevelData) {
    this.variantLevelData = variantLevelData;
  }

  public CaseLevelData[] getCaseLevelData() {
    return caseLevelData;
  }

  public void setCaseLevelData(CaseLevelData[] caseLevelData) {
    this.caseLevelData = caseLevelData;
  }
}
