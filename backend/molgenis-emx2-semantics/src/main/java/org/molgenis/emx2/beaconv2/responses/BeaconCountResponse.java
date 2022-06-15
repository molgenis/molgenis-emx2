package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Handover;

public class BeaconCountResponse {
  BeaconResponseMeta meta;
  BeaconSummaryResponseSection responseSummary;
  Map<String, Object> info;
  Handover[] beaconHandovers;
}
