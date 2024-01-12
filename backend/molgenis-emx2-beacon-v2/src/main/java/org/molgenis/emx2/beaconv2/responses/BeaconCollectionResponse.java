package org.molgenis.emx2.beaconv2.responses;

import org.molgenis.emx2.beaconv2.common.misc.Handover;
import org.molgenis.emx2.beaconv2.common.misc.Info;

public class BeaconCollectionResponse {
  private BeaconResponseMeta meta;
  private BeaconSummaryResponseSection responseSummary;
  private Info info;
  private Response response;
  private Handover[] handovers;

  private class Response {
    // this is where the model defined bit goes
    // should have for a example Dataset list
    Object[] collections;
  }
}
