package org.molgenis.emx2.beacon.common;

import java.util.Map;

public class Info {
  BeaconInformationalResponseMeta meta;
  BeaconInfoResults response;

  public static class BeaconInfoResults {
    String id;
    String name;
    String apiVersion;
    BeaconEnvironment environment;
    BeaconOrganization organization;

    public enum BeaconEnvironment {
      PROD,
      TEST,
      DEV,
      STAGING;

      public String toString() {
        return name().toLowerCase();
      }
    }

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
