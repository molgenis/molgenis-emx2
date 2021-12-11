package org.molgenis.emx2.beacon.configuration;

public class BeaconMapSchema {
  String $schema;
  EndPoint endpointSets;

  public static class EndPoint {
    String entryType;
    // wow, that is heavy
    String openAPIEndpointsDefinition;
    String rootUrl;
    String singleEntryUrl;
  }
}
