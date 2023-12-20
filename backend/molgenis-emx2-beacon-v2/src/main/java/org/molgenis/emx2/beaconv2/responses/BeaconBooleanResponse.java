package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.misc.Handover;

public class BeaconBooleanResponse {
  private BeaconResponseMeta meta;
  private BooleanResponseSummary responseSummary;
  private Map<String, Object> info;
  private Handover[] beaconHandovers;

  private class BooleanResponseSummary {
    // Indicator of whether any entry was observed. This should be non-null, unless there was an
    // error, in which case an error response is expected instead of this one.
    boolean exists;
  }
}
