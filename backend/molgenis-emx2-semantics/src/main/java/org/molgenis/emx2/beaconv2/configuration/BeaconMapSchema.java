package org.molgenis.emx2.beaconv2.configuration;

public class BeaconMapSchema {
  private String $schema;
  private EndPoint endpointSets;

  public static class EndPoint {
    String entryType;
    // wow, that is heavy
    String openAPIEndpointsDefinition;
    String rootUrl;
    String singleEntryUrl;
  }
}
