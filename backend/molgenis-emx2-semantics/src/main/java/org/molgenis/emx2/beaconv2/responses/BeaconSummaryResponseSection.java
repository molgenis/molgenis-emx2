package org.molgenis.emx2.beaconv2.responses;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconSummaryResponseSection.json
public class BeaconSummaryResponseSection {
  // "Indicator of whether any entry was observed. This should be non-null, unless there was an
  // error, in which case an error response is expected instead of this one.
  boolean exists;

  // Total number of results
  int numTotalResults;
}
