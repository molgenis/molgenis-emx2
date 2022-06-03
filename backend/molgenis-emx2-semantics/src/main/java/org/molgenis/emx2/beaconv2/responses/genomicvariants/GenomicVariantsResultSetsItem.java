package org.molgenis.emx2.beaconv2.responses.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariantsResultSetsItem {
  String variantInternalId;
  String variantType;
  String referenceBases;
  String alternateBases;
  Position position;

  public GenomicVariantsResultSetsItem() {
    super();
    position = new Position();
  }

  public GenomicVariantsResultSetsItem(
      String variantInternalId,
      String variantType,
      String referenceBases,
      String alternateBases,
      Position position) {
    this.variantInternalId = variantInternalId;
    this.variantType = variantType;
    this.alternateBases = alternateBases;
    this.referenceBases = referenceBases;
    this.position = position;
  }
}
