package org.molgenis.emx2.datamodels.profiles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonDeserialize(converter = PostProcessProfiles.class) // invoked after class is fully deserialized
public class Profiles {

  /*
  Variables mapped to the YAML file
  FIXME: is it possible to directly load CSV into Lists using a mapper?
   */
  public String name;
  public String description;
  public String profileTags;
  public String demoData;
  public String settings;
  public List<String> profileTagsList;
  public List<String> demoDataList;
  public List<String> settingsList;

  // special options
  public String ontologiesToFixedSchema;
  public String setViewPermission;
}
