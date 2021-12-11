package org.molgenis.emx2.beacon.responses;

import org.molgenis.emx2.beacon.common.Handover;
import org.molgenis.emx2.beacon.common.Info;

public class BeaconCollectionResponse {
  BeaconResponseMeta meta;
  BeaconSummaryResponseSection responseSummary;
  Info info;
  Response response;
  Handover[] handovers;

  private class Response {
    // this is where the model defined bit goes
    // should have for a example Dataset list
    Object[] collections;
  }
}
