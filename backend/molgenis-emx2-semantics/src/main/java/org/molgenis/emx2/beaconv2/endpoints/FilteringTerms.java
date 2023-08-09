package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.beaconv2.common.misc.Handover;
import org.molgenis.emx2.beaconv2.endpoints.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ResponseSummary;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTermsResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTerms {

  private DatasetsMeta meta;
  private ResponseSummary responseSummary;
  private FilteringTermsResponse response;
  private Handover beaconHandovers;

  public FilteringTerms(Database database) {
    this.meta = new DatasetsMeta("../beaconFilteringTermsResponse.json", "filteringterms");
    this.response = new FilteringTermsResponse(database);
    this.responseSummary = new ResponseSummary();
    this.beaconHandovers = new Handover();
  }

  public DatasetsMeta getMeta() {
    return meta;
  }

  public ResponseSummary getResponseSummary() {
    return responseSummary;
  }

  public FilteringTermsResponse getResponse() {
    return response;
  }

  public Handover getBeaconHandovers() {
    return beaconHandovers;
  }
}
