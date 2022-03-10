package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.beaconv2.responses.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.responses.datasets.DatasetsResponse;
import org.molgenis.emx2.beaconv2.responses.datasets.ResponseSummary;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Datasets {

  DatasetsMeta meta;
  ResponseSummary responseSummary;
  DatasetsResponse response;

  public Datasets(Schema schema) {
    this.meta = new DatasetsMeta("../beaconDatasetResponse.json", "datasets");
    this.response = new DatasetsResponse(schema);
    this.responseSummary = new ResponseSummary();
  }
}
