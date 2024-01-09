package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ResponseSummary;
import org.molgenis.emx2.beaconv2.endpoints.genomicvariants.GenomicVariantsResponse;
import spark.Request;

/** Genomic variants, analyses, cohorts, sequencing runs, individuals, samples */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenomicVariants {

  private DatasetsMeta meta;
  private ResponseSummary responseSummary;
  private GenomicVariantsResponse response;

  public GenomicVariants(Request request, List<Table> genomicVariantTables) throws Exception {
    this.meta = new DatasetsMeta("../beaconDatasetResponse.json", "datasets");
    this.responseSummary = new ResponseSummary();
    this.response = new GenomicVariantsResponse(request, genomicVariantTables);
  }
}
