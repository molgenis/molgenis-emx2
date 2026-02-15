package org.molgenis.emx2;

import java.util.List;

public class ColumnAccess {
  private List<String> editable;
  private List<String> readonly;
  private List<String> hidden;

  public ColumnAccess() {}

  public List<String> getEditable() {
    return editable;
  }

  public ColumnAccess setEditable(List<String> editable) {
    this.editable = editable;
    return this;
  }

  public List<String> getReadonly() {
    return readonly;
  }

  public ColumnAccess setReadonly(List<String> readonly) {
    this.readonly = readonly;
    return this;
  }

  public List<String> getHidden() {
    return hidden;
  }

  public ColumnAccess setHidden(List<String> hidden) {
    this.hidden = hidden;
    return this;
  }
}
