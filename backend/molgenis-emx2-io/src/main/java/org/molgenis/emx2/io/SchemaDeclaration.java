package org.molgenis.emx2.io;

import java.net.URL;
import java.util.List;

public class SchemaDeclaration {
  private String name;
  private String description;
  private List<URL> sourceURLs;

  public SchemaDeclaration(String name, String description, List<URL> sourceURLs) {
    this.name = name;
    this.description = description;
    this.sourceURLs = sourceURLs;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public List<URL> getSourceURLs() {
    return sourceURLs;
  }
}
