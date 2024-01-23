package org.molgenis.emx2.datamodels.profiles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonDeserialize(converter = PostProcessProfiles.class) // invoked after class is fully deserialized
public class Profiles {

  /*
  Variables mapped to the YAML file
  FIXME: is it possible to directly load CSV into Lists using a mapper?
   */
  private String name;
  private String description;
  private String profileTags;
  private String demoData;
  private String settings;
  private List<String> profileTagsList;
  private List<String> demoDataList;
  private List<String> settingsList;

  // special options
  private String ontologiesToFixedSchema;
  private String setViewPermission;
  private String setEditPermission;
  private List<CreateSchemas> firstCreateSchemasIfMissing;

  public String getSetEditPermission() {
    return setEditPermission;
  }

  public void setSetEditPermission(String setEditPermission) {
    this.setEditPermission = setEditPermission;
  }

  public List<CreateSchemas> getFirstCreateSchemasIfMissing() {
    return firstCreateSchemasIfMissing;
  }

  public void setFirstCreateSchemasIfMissing(List<CreateSchemas> firstCreateSchemasIfMissing) {
    this.firstCreateSchemasIfMissing = firstCreateSchemasIfMissing;
  }

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

  protected String getProfileTags() {
    return profileTags;
  }

  protected void setProfileTags(String profileTags) {
    this.profileTags = profileTags;
  }

  protected String getDemoData() {
    return demoData;
  }

  protected void setDemoData(String demoData) {
    this.demoData = demoData;
  }

  protected String getSettings() {
    return settings;
  }

  protected void setSettings(String settings) {
    this.settings = settings;
  }

  protected List<String> getProfileTagsList() {
    return profileTagsList;
  }

  public void setProfileTagsList(List<String> profileTagsList) {
    this.profileTagsList = profileTagsList;
  }

  public List<String> getDemoDataList() {
    return demoDataList;
  }

  public void setDemoDataList(List<String> demoDataList) {
    this.demoDataList = demoDataList;
  }

  public List<String> getSettingsList() {
    return settingsList;
  }

  public void setSettingsList(List<String> settingsList) {
    this.settingsList = settingsList;
  }

  public String getOntologiesToFixedSchema() {
    return ontologiesToFixedSchema;
  }

  public void setOntologiesToFixedSchema(String ontologiesToFixedSchema) {
    this.ontologiesToFixedSchema = ontologiesToFixedSchema;
  }

  public String getSetViewPermission() {
    return setViewPermission;
  }

  public void setSetViewPermission(String setViewPermission) {
    this.setViewPermission = setViewPermission;
  }
}
