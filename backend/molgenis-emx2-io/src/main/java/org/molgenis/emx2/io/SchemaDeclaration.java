package org.molgenis.emx2.io;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.molgenis.emx2.MolgenisException;

public class SchemaDeclaration {
  private String name;
  private String description;
  private List<URL> sourceURLs;

  public SchemaDeclaration(String name) {
    this.setName(name);
  }

  public SchemaDeclaration(String name, String description, List<URL> sourceURLs) {
    this(name);
    this.setDescription(description);
    this.setSourceURLs(sourceURLs);
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

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setSourceURLs(List<URL> sourceURLs) {
    Objects.requireNonNull(sourceURLs, "sourceURLs cannot be null");
    this.sourceURLs = sourceURLs;
  }

  public void setSourceURLsFromStrings(List<String> sourceURLs) {
    Objects.requireNonNull(sourceURLs, "sourceURLs cannot be null");
    List<URL> result = new ArrayList<>();
    for (String url : sourceURLs) {
      try {
        result.add(new URL(url));
      } catch (Exception e) {
        throw new MolgenisException("invalid sourceURL '" + url + "' " + e.getMessage());
      }
    }
    this.sourceURLs = result;
  }
}
