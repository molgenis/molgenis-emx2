package org.molgenis.emx2.fairmapper.dcat;

import java.util.ArrayList;
import java.util.List;

public class HarvestReport {
  private int resourcesImported;
  private final List<String> warnings = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();

  public void incrementResources(int count) {
    this.resourcesImported += count;
  }

  public void addWarning(String warning) {
    this.warnings.add(warning);
  }

  public void addError(String error) {
    this.errors.add(error);
  }

  public int getResourcesImported() {
    return resourcesImported;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public List<String> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
