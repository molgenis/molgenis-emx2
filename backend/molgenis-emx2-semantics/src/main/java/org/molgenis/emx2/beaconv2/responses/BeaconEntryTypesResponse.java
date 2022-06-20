package org.molgenis.emx2.beaconv2.responses;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.EntryType;
import org.molgenis.emx2.beaconv2.common.misc.BeaconInformationalResponseMeta;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/beaconEntryTypesResponse.json
public class BeaconEntryTypesResponse {

  // Information about the response that could be relevant for the Beacon client in order to
  // interpret the results.
  private BeaconInformationalResponseMeta meta;

  // This is a dictionary of the entry types implemented in this Beacon instance.
  // https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/configuration/entryTypesSchema.json
  private Map<String, EntryType> entryTypes;
}
