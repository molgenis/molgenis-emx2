package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Collections;
import java.util.Map;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.beaconv2.common.misc.Handover;
import org.molgenis.emx2.beaconv2.endpoints.datasets.DatasetsMeta;
import org.molgenis.emx2.beaconv2.endpoints.datasets.ResponseSummary;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTerm;
import org.molgenis.emx2.beaconv2.endpoints.filteringterms.FilteringTermsFetcher;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class FilteringTerms {

  private DatasetsMeta meta;
  private ResponseSummary responseSummary;
  private Map<String, FilteringTerm[]> response;
  private Handover beaconHandovers;

  public FilteringTerms(Database database) {
    database.tx(
        db -> {
          this.meta = new DatasetsMeta("../beaconFilteringTermsResponse.json", "filteringterms");
          FilteringTerm[] terms = new FilteringTermsFetcher(database).getAllFilteringTerms();
          this.response = Collections.singletonMap("filteringTerms", terms);
          this.responseSummary = new ResponseSummary();
          this.beaconHandovers = new Handover();
        });
  }

  public DatasetsMeta getMeta() {
    return meta;
  }

  public ResponseSummary getResponseSummary() {
    return responseSummary;
  }

  public Map<String, FilteringTerm[]> getResponse() {
    return response;
  }

  public Handover getBeaconHandovers() {
    return beaconHandovers;
  }
}
