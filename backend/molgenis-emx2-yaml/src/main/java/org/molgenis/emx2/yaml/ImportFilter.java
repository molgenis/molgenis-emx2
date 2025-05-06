package org.molgenis.emx2.yaml;

import java.util.List;

public class ImportFilter {
  private List<String> fields;
  private List<String> subclasses;
  private List<String> tags;

  public List<String> getFields() {
    return fields;
  }

  public void setFields(List<String> fields) {
    this.fields = fields;
  }

  public List<String> getSubclasses() {
    return subclasses;
  }

  public void setSubclasses(List<String> subclasses) {
    this.subclasses = subclasses;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }
}
