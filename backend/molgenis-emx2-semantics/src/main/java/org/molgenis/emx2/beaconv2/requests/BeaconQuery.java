package org.molgenis.emx2.beaconv2.requests;

import static org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses.HIT;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.endpoints.datasets.Pagination;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconQuery {
  private String description;
  private BeaconRequestParameters[] requestParameters;
  private Filter[] filters;
  private IncludedResultsetResponses includeResultsetResponses = HIT;
  private Pagination pagination = new Pagination();
  private Granularity requestGranularity;
  private boolean testMode;

  public String getDescription() {
    return description;
  }

  public BeaconRequestParameters[] getRequestParameters() {
    return requestParameters;
  }

  public Filter[] getFilters() throws JsonProcessingException {
    // post processing on the value needed to support both 'string' and 'string array' input as
    // required per spec
    for (Filter filter : filters) {
      filter.parseValues();
    }
    return filters;
  }

  public IncludedResultsetResponses getIncludeResultsetResponses() {
    return includeResultsetResponses;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public Granularity getRequestGranularity() {
    return requestGranularity;
  }

  public boolean isTestMode() {
    return testMode;
  }
}
