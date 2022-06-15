package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Granularity;
import org.molgenis.emx2.beaconv2.common.misc.IncludedResultsetResponses;
import org.molgenis.emx2.beaconv2.common.misc.Pagination;
import org.molgenis.emx2.beaconv2.common.misc.SchemasPerEntity;
import org.molgenis.emx2.beaconv2.requests.BeaconRequestBody;

public class BeaconResponseMeta {
  String beaconId;
  String apiVersion;
  SchemasPerEntity[] returnedSchemas;
  Granularity returnedGranularity;
  ReceivedRequestSummary receivedRequestSummary;

  public static class ReceivedRequestSummary {
    String apiVersion;
    SchemasPerEntity[] requestedSchemas;
    Pagination pagination;
    Granularity requestedGranularity;
    BeaconRequestBody.BeaconFilteringTerms.Filter[] filters;
    Map<String, Object> requestParameters; // this is nasty
    IncludedResultsetResponses includeResultsetResponses;
    boolean testMode = false;
  }
}
