package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.common.misc.Pagination;
import org.molgenis.emx2.beaconv2.common.misc.SchemasPerEntity;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

public class BeaconResponseMeta {
  private String beaconId;
  private String apiVersion;
  private SchemasPerEntity[] returnedSchemas;
  private Granularity returnedGranularity;
  private ReceivedRequestSummary receivedRequestSummary;

  public static class ReceivedRequestSummary {
    private String apiVersion;
    private SchemasPerEntity[] requestedSchemas;
    private Pagination pagination;
    private Granularity requestedGranularity;
    private BeaconRequestBody.BeaconFilteringTerms.Filter[] filters;
    private Map<String, Object> requestParameters; // this is nasty
    private IncludedResultsetResponses includeResultsetResponses;
    private boolean testMode = false;
  }
}
