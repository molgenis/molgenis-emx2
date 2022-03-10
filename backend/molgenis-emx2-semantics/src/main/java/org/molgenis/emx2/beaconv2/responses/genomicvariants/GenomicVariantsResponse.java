package org.molgenis.emx2.beaconv2.responses.genomicvariants;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariantsResponse {

  GenomicVariantsResultSets[] resultSets = new GenomicVariantsResultSets[] {};

  public GenomicVariantsResponse() {
    List<GenomicVariantsResultSets> rList = new ArrayList<>();
    rList.add(new GenomicVariantsResultSets()); // TODO placeholder empty result set
    this.resultSets = rList.toArray(new GenomicVariantsResultSets[rList.size()]);
  }
}
