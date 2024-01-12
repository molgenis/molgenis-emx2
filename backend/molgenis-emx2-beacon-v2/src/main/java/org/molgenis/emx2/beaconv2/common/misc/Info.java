package org.molgenis.emx2.beaconv2.common.misc;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.BeaconEnvironment;

public class Info {
  private BeaconInformationalResponseMeta meta;
  private BeaconInfoResults response;

  public static class BeaconInfoResults {
    private String id;
    private String name;
    private String apiVersion;
    private BeaconEnvironment environment;
    private BeaconOrganization organization;

    public static class BeaconOrganization {
      private String id;
      private String name;
      private String description;
      private String address;
      private String welcomeUrl;
      private String contactUrl;
      private String logoUrl;
      private Map<String, String> info;
    }
  }
}
