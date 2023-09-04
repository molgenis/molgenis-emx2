package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.cohorts.CohortsResponse;
import org.molgenis.emx2.beaconv2.endpoints.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ResponseSummary;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Cohorts {

  private DatasetsMeta meta;
  private ResponseSummary responseSummary;
  private CohortsResponse response;

  public Cohorts(Request request, List<Table> tables) throws Exception {
    this.meta = new DatasetsMeta("../beaconDatasetResponse.json", "datasets");
    this.response = new CohortsResponse(request, tables);
    this.responseSummary = new ResponseSummary();
  }
}
