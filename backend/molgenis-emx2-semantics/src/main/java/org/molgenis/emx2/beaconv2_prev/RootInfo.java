package org.molgenis.emx2.beaconv2_prev;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beacon.common.BeaconEnvironment;
import org.molgenis.emx2.beacon.common.SchemasPerEntity;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RootInfo {

  String $schema = "../beaconInfoResponse.json";
  BeaconInformationalResponseMeta meta = new BeaconInformationalResponseMeta();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class BeaconInformationalResponseMeta {
    String beaconId = "demo";
    String apiVersion = "xx";
    SchemasPerEntity[] returnedSchemas;
  }

  Response response = new Response();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class Response {
    String id = "org.ga4gh.beacon";
    String name = "resp-name";
    String apiVersion = "v0.3";
    String environment = BeaconEnvironment.DEV.toString();
    BeaconOrganization organization = new BeaconOrganization();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class BeaconOrganization {
      String id = "org-id";
      String name = "org-name";
    }
  }
}
