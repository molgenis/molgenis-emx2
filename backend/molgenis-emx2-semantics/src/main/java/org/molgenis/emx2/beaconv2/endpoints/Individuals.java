package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.beaconv2.endpoints.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ResponseSummary;
import org.molgenis.emx2.beaconv2.endpoints.individuals.IndividualsResponse;
import spark.Request;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Individuals {

  private DatasetsMeta meta;
  private ResponseSummary responseSummary;
  private IndividualsResponse response;

  public Individuals(Request request, List<Table> tables) throws Exception {
    this.meta = new DatasetsMeta("../beaconDatasetResponse.json", "datasets");
    this.response = new IndividualsResponse(request, tables);
    this.responseSummary = new ResponseSummary();
  }
}
