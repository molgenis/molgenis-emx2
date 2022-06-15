package org.molgenis.emx2.beaconv2.responses;

import org.molgenis.emx2.beaconv2.common.misc.Handover;
import org.molgenis.emx2.beaconv2.common.misc.Info;

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
