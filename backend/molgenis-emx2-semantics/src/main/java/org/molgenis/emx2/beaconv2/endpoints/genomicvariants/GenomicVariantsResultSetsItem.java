package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariantsResultSetsItem {
  String variantInternalId;
  String variantType;
  String referenceBases;
  String alternateBases;
  Position position;
  String geneId;

  public GenomicVariantsResultSetsItem() {
    super();
    position = new Position();
  }

  public GenomicVariantsResultSetsItem(
      String variantInternalId,
      String variantType,
      String referenceBases,
      String alternateBases,
      Position position,
      String geneId) {
    this.variantInternalId = variantInternalId;
    this.variantType = variantType;
    this.alternateBases = alternateBases;
    this.referenceBases = referenceBases;
    this.position = position;
    this.geneId = geneId;
  }
}
