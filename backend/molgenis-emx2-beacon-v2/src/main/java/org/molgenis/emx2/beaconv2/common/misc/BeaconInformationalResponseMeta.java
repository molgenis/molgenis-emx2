package org.molgenis.emx2.beaconv2.common.misc;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconInformationalResponseMeta.json
public class BeaconInformationalResponseMeta {
  // Identifier of the beacon, as defined in `Beacon`.
  private String beaconId = "demo";

  // Version of the API.
  private String apiVersion = "xx";

  // Set of schemas to be used in the response to a request
  // entities in this case are 'dataset' (not the tables, but the exposed endpoints)
  private SchemasPerEntity[] returnedSchemas;

  /** generated getters below, needed for Jackson */
  private String getBeaconId() {
    return beaconId;
  }

  private String getApiVersion() {
    return apiVersion;
  }

  private SchemasPerEntity[] getReturnedSchemas() {
    return returnedSchemas;
  }
}
