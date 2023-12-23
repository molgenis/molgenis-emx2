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
  public String profiles;
  public String data;
  public String examples;
  public List<String> profilesList;
  public List<String> dataList;
  public List<String> examplesList;

  // special options
  public String dataToFixedSchema;
  public String setViewPermission;
}
