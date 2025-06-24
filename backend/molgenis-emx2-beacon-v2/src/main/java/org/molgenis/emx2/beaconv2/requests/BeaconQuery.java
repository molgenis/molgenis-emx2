package org.molgenis.emx2.beaconv2.requests;

import static org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses.HIT;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.molgenis.emx2.beaconv2.EntryType;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.endpoints.datasets.Pagination;
import org.molgenis.emx2.beaconv2.filter.Filter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconQuery {
  private String description;
  private Map<String, BeaconRequestParameters> requestParameters = new HashMap<>();
  private List<Filter> filters = new ArrayList<>();
  private IncludedResultsetResponses includeResultsetResponses = HIT;
  private Pagination pagination = new Pagination();
  private Granularity requestedGranularity = Granularity.RECORD;
  private boolean testMode;
  private EntryType entryType;

  public BeaconQuery() {}

  public String getDescription() {
    return description;
  }

  public List<BeaconRequestParameters> getRequestParameters() {
    return requestParameters.values().stream().toList();
  }

  public void setRequestParameters(Map<String, String> requestParameters) {
    for (var entry : requestParameters.entrySet()) {
      this.requestParameters.put(
          entry.getKey(), new BeaconRequestParameters(entry.getKey(), entry.getValue()));
    }
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

  public EntryType getEntryType() {
    return entryType;
  }

  public Map<String, BeaconRequestParameters> getRequestParametersMap() {
    return requestParameters;
  }

  public EntryType addUrlParameters(Context ctx) {
    Map<String, String> params = ctx.pathParamMap();
    if (params.containsKey("entry_type")) {
      entryType = EntryType.findByName(params.get("entry_type"));
    }
    for (var urlParam : params.entrySet()) {
      String ref = urlParam.getKey().replaceAll(":", "");
      requestParameters.put(ref, new BeaconRequestParameters(ref, urlParam.getValue()));
    }
    for (var queryParam : ctx.queryParamMap().entrySet()) {
      if (queryParam.getKey().equalsIgnoreCase("limit")) {
        pagination.setLimit(Integer.parseInt(queryParam.getValue().get(0)));
      } else if (queryParam.getKey().equalsIgnoreCase("skip")) {
        pagination.setSkip(Integer.parseInt(queryParam.getValue().get(0)));
      } else if (queryParam.getKey().equalsIgnoreCase("requestedGranularity")) {
        requestedGranularity = Granularity.fromString(queryParam.getValue().get(0));
      } else {
        requestParameters.put(
            queryParam.getKey(),
            new BeaconRequestParameters(queryParam.getKey(), queryParam.getValue().get(0)));
      }
    }

    return this.entryType;
  }
}
