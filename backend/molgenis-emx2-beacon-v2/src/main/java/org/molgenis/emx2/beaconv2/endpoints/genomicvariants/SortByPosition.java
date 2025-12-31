package org.molgenis.emx2.beaconv2.endpoints.genomicvariants;

import java.util.Comparator;

public class SortByPosition implements Comparator<GenomicVariant> {
  @Override
  public int compare(GenomicVariant o1, GenomicVariant o2) {
    if (o1.position().getStart().length > 1 || o2.position().getStart().length > 1) {
      throw new AssertionError(
          "Sorting on SVs with uncertain starting coordinates currently not implemented");
    }
    return Math.toIntExact(o1.position().getStart()[0] - o2.position().getStart()[0]);
  }
}
