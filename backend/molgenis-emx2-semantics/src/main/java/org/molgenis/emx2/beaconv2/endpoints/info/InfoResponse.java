package org.molgenis.emx2.beaconv2.endpoints.info;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.BeaconEnvironment;
import org.molgenis.emx2.beaconv2.endpoints.configuration.Organization;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InfoResponse {
  private String id = "molgenis";
  private String name = "MOLGENIS Beacon v2";
  private String apiVersion = "v2.0.0-draft.4"; // TODO: from static, but not allowed?
  private BeaconEnvironment environment = BeaconEnvironment.prod;
  private Organization organization = new Organization();
  private String description = "This Beacon is based on the GA4GH Beacon v2.0";
  private String version = "v2";
  private String welcomeUrl = "https://www.molgenis.org/beacon";
  private String createDateTime = "2022-01-01";
  private String updateDateTime = "2022-01-01";
}
