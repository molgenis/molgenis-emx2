package org.molgenis.emx2.datamodels.profiles;

public class CreateSchemas {

  private String name;
  private String profile;
  private boolean importDemoData;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProfile() {
    return profile;
  }

  public void setProfile(String profile) {
    this.profile = profile;
  }

  public boolean isImportDemoData() {
    return importDemoData;
  }

  public void setImportDemoData(boolean importDemoData) {
    this.importDemoData = importDemoData;
  }
}
