package org.molgenis.emx2.beacon.responses;

import java.util.Map;
import org.molgenis.emx2.beacon.common.Handover;

public class BeaconCountResponse {
  BeaconResponseMeta meta;
  BeaconSummaryResponseSection responseSummary;
  Map<String, Object> info;
  Handover[] beaconHandovers;
}
