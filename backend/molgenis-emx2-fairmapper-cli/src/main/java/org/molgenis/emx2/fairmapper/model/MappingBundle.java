package org.molgenis.emx2.fairmapper.model;

import java.util.List;

public record MappingBundle(String name, String version, List<Mapping> mappings) {

  public List<Mapping> getMappings() {
    return mappings != null ? mappings : List.of();
  }
}
