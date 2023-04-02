package org.molgenis.emx2.io.yaml;

import java.util.Map;

public class YamlBase {
  private String uri; // can be a list
  private String name;
  private String description;
  private String imports;
  private Map<String, String> rdf;
  private Map<String, YamlAuthor> authors;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public Map<String, String> getRdf() {
    return rdf;
  }

  public void setRdf(Map<String, String> rdf) {
    this.rdf = rdf;
  }

  public String getImports() {
    return imports;
  }

  public void setImports(String imports) {
    this.imports = imports;
  }

  public Map<String, YamlAuthor> getAuthors() {
    return authors;
  }

  public void setAuthors(Map<String, YamlAuthor> authors) {
    this.authors = authors;
  }
}
