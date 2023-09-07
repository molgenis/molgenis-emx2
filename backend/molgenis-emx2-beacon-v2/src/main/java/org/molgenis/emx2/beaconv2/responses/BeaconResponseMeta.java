package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.common.misc.SchemasPerEntity;
import org.molgenis.emx2.beaconv2.endpoints.datasets.Pagination;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;
import org.molgenis.emx2.beaconv2.requests.Filter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconResponseMeta {
  private String beaconId;
  private String apiVersion;
  private SchemasPerEntity[] returnedSchemas;
  private String returnedGranularity;
  private BeaconRequestBody receivedRequestSummary;

  public BeaconResponseMeta(
      String host, String entityType, String granularity, BeaconRequestBody receivedRequest) {
    this.beaconId = host;
    this.apiVersion = "v2.0";
    this.returnedGranularity = granularity;
    this.receivedRequestSummary = receivedRequest;
    this.returnedSchemas =
        new SchemasPerEntity[] {new SchemasPerEntity(entityType, "ga4gh-beacon-individual-v2.0.0")};
  }

  public static class ReceivedRequestSummary {
    private String apiVersion;
    private SchemasPerEntity[] requestedSchemas;
    private Pagination pagination;
    private Granularity requestedGranularity;
    private Filter[] filters;
    private Map<String, Object> requestParameters; // this is nasty
    private IncludedResultsetResponses includeResultsetResponses;
    private boolean testMode = false;
  }
}
