package org.molgenis.emx2.beaconv2.common.misc;

// https://github.com/ga4gh-beacon/beacon-framework-v2/blob/main/responses/sections/beaconInformationalResponseMeta.json
public class BeaconInformationalResponseMeta {
  // Identifier of the beacon, as defined in `Beacon`.
  String beaconId = "demo";

  // Version of the API.
  String apiVersion = "xx";

  // Set of schemas to be used in the response to a request
  // entities in this case are 'dataset' (not the tables, but the exposed endpoints)
  SchemasPerEntity[] returnedSchemas;

  /** generated getters below, needed for Jackson */
  public String getBeaconId() {
    return beaconId;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public SchemasPerEntity[] getReturnedSchemas() {
    return returnedSchemas;
  }
}
