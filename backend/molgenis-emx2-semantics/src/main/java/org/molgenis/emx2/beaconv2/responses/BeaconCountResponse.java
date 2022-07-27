package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Handover;

public class BeaconCountResponse {
  private BeaconResponseMeta meta;
  private BeaconSummaryResponseSection responseSummary;
  private Map<String, Object> info;
  private Handover[] beaconHandovers;
}
