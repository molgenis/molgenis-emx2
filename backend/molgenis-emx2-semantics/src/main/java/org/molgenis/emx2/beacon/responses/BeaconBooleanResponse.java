package org.molgenis.emx2.beacon.responses;

public class BeaconBooleanResponse extends BeaconQueryResponse {
  BeaconBooleanResponseSection responseSummary;

  private class BeaconBooleanResponseSection {
    // Indicator of whether any entry was observed. This should be non-null, unless there was an
    // error, in which case an error response is expected instead of this one.
    boolean exists;
  }
}
