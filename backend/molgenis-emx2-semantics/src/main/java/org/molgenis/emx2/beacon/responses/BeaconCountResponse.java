package org.molgenis.emx2.beacon.responses;

import org.molgenis.emx2.beacon.common.Handover;

import java.util.Map;

public class BeaconCountResponse {
  BeaconResponseMeta meta;
  BeaconSummaryResponseSection responseSummary;
  Map<String, Object> info;
  Handover[] beaconHandovers;
}
