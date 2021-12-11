package org.molgenis.emx2.beacon.common;

import org.molgenis.emx2.beacon.common.SchemasPerEntity;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconInformationalResponseMeta.json
public class BeaconInformationalResponseMeta {
  // Identifier of the beacon, as defined in `Beacon`.
  String beaconId;

  // Version of the API.
  String apiVersion;

  // Set of schemas to be used in the response to a request
  SchemasPerEntity[] returnedSchemas;
}
