package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Arrays;
import org.molgenis.emx2.beaconv2.common.Schemas;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestParameters;
import org.molgenis.emx2.beaconv2.requests.Filter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ReceivedRequestSummary {

  private String apiVersion;
  private String requestedGranularity;
  private Pagination pagination;
  private Schemas[] requestedSchemas;
  private BeaconRequestParameters requestParameters;
  private String includeResultsetResponses;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private boolean testMode;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Filter[] filters;

  public ReceivedRequestSummary(String reqSchemas) {
    this.apiVersion = "v2.0.0";
    this.requestedGranularity = "record";
    this.pagination = new Pagination();
    this.requestedSchemas = Arrays.asList(new Schemas(reqSchemas)).toArray(Schemas[]::new);
    this.filters = new Filter[] {};
    this.requestParameters = new BeaconRequestParameters();
    this.requestParameters.empty();
    includeResultsetResponses = "HIT";
    this.testMode = false;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public String getRequestedGranularity() {
    return requestedGranularity;
  }

  public Pagination getPagination() {
    return pagination;
  }

  public Schemas[] getRequestedSchemas() {
    return requestedSchemas;
  }

  public BeaconRequestParameters getRequestParameters() {
    return requestParameters;
  }

  public String getIncludeResultsetResponses() {
    return includeResultsetResponses;
  }

  public boolean isTestMode() {
    return testMode;
  }

  public Filter[] getFilters() {
    return filters;
  }
}
