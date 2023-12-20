package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconSummaryResponseSection.json
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BeaconSummaryResponseSection {
  // "Indicator of whether any entry was observed. This should be non-null, unless there was an
  // error, in which case an error response is expected instead of this one.
  private String exists;

  // Total number of results
  private int numTotalResults;

  public BeaconSummaryResponseSection(boolean exists, int numTotalResults) {
    this.exists = exists + "";
    this.numTotalResults = numTotalResults;
  }
}
