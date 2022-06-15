package org.molgenis.emx2.beaconv2.common.misc;

import java.util.Map;
import org.molgenis.emx2.beaconv2.common.BeaconEnvironment;

public class Info {
  BeaconInformationalResponseMeta meta;
  BeaconInfoResults response;

  public static class BeaconInfoResults {
    String id;
    String name;
    String apiVersion;
    BeaconEnvironment environment;
    BeaconOrganization organization;

    public static class BeaconOrganization {
      String id;
      String name;
      String description;
      String address;
      String welcomeUrl;
      String contactUrl;
      String logoUrl;
      Map<String, String> info;
    }
  }
}
