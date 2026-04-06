package org.molgenis.emx2;

import java.util.List;

public class ProfileEntry {
  private final String id;
  private final String description;
  private final List<String> includes;

  public ProfileEntry(String id, String description, List<String> includes) {
    this.id = id;
    this.description = description;
    this.includes = includes != null ? includes : List.of();
  }

  public String getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getIncludes() {
    return includes;
  }
}
