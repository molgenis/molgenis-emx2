package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.endpoints.datasets.DatasetsResponse;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ResponseSummary;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Datasets {

  private DatasetsMeta meta;
  private ResponseSummary responseSummary;
  private DatasetsResponse response;

  public Datasets(Request request, List<Table> tables) throws Exception {
    this.meta = new DatasetsMeta("../beaconDatasetResponse.json", "datasets");
    this.response = new DatasetsResponse(request, tables);
    this.responseSummary = new ResponseSummary();
  }
}
