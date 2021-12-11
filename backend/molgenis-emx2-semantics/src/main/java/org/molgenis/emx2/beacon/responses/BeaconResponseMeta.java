package org.molgenis.emx2.beacon.responses;

import org.molgenis.emx2.beacon.common.Granularity;
import org.molgenis.emx2.beacon.common.IncludedResultsetResponses;
import org.molgenis.emx2.beacon.common.Pagination;
import org.molgenis.emx2.beacon.common.SchemasPerEntity;
import org.molgenis.emx2.beacon.requests.BeaconRequestBody;

import java.util.Map;

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
