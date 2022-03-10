package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.responses.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.responses.datasets.ResponseSummary;
import org.molgenis.emx2.beaconv2.responses.genomicvariants.GenomicVariantsResponse;

/** Genomic variants, analyses, cohorts, sequencing runs, individuals, samples */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariants {

  DatasetsMeta meta;
  ResponseSummary responseSummary;
  GenomicVariantsResponse response;

  public GenomicVariants() {
    this.meta = new DatasetsMeta("../beaconDatasetResponse.json", "datasets");
    this.response = new GenomicVariantsResponse();
    this.responseSummary = new ResponseSummary();
  }
}
