package org.molgenis.emx2.fairmapper.model;

import java.util.List;

public record MappingBundle(
    String apiVersion, String kind, Metadata metadata, List<Endpoint> endpoints) {

  public record Metadata(String name, String version) {}
}
