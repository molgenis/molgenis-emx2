package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import java.util.Comparator;

public class SortByPosition implements Comparator<GenomicVariantsResultSetsItem> {
  @Override
  public int compare(GenomicVariantsResultSetsItem o1, GenomicVariantsResultSetsItem o2) {
    if (o1.getPosition().getStart().length > 1 || o2.getPosition().getStart().length > 1) {
      throw new AssertionError(
          "Sorting on SVs with uncertain starting coordinates currently not implemented");
    }
    return Math.toIntExact(o1.getPosition().getStart()[0] - o2.getPosition().getStart()[0]);
  }
}
