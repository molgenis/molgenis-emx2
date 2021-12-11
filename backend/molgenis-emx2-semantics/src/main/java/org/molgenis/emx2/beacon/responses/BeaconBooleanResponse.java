package org.molgenis.emx2.beacon.responses;

import java.util.Map;
import org.molgenis.emx2.beacon.common.Handover;

public class BeaconBooleanResponse {
  BeaconResponseMeta meta;
  BooleanResponseSummary responseSummary;
  Map<String, Object> info;
  Handover[] beaconHandovers;

  private class BooleanResponseSummary {
    // Indicator of whether any entry was observed. This should be non-null, unless there was an
    // error, in which case an error response is expected instead of this one.
    boolean exists;
  }
}
