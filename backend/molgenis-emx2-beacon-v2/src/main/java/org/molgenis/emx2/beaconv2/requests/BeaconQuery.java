package org.molgenis.emx2.beaconv2.requests;

import static org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses.HIT;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.endpoints.datasets.Pagination;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconQuery {
  private String description;
  private List<BeaconRequestParameters> requestParameters;
  private List<Filter> filters = new ArrayList<>();
  private IncludedResultsetResponses includeResultsetResponses = HIT;
  private Pagination pagination = new Pagination();
  private Granularity requestedGranularity = Granularity.UNDEFINED;
  private boolean testMode;

  public BeaconQuery() {}

  public BeaconQuery(Map<String, String> params) {
    if (requestParameters == null) requestParameters = new ArrayList<>();

    for (var entry : params.entrySet()) {
      String ref = entry.getKey().replaceAll(":", "");
      requestParameters.add(new BeaconRequestParameters(ref, entry.getValue()));
    }
  }

  public String getDescription() {
    return description;
  }

  public List<BeaconRequestParameters> getRequestParameters() {
    return requestParameters;
  }

  public List<Filter> getFilters() {
    return filters;
  }

  public IncludedResultsetResponses getIncludeResultsetResponses() {
    return includeResultsetResponses;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public Granularity getRequestedGranularity() {
    return requestedGranularity;
  }

  public boolean isTestMode() {
    return testMode;
  }
}
