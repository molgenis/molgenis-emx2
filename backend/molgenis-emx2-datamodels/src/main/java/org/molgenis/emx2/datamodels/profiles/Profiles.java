package org.molgenis.emx2.datamodels.profiles;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonDeserialize(converter = PostProcessProfiles.class) // invoked after class is fully deserialized
public class Profiles {

  /*
  Variables mapped to the YAML file
   */
  public String name;
  public String description;
  public String datamodels;
  public String profiles;
  public List<String> datamodelsList;
  public List<String> profilesList;

  @Override
  public String toString() {
    return "Profiles{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", datamodels="
        + datamodelsList
        + ", profiles="
        + profilesList
        + '}';
  }
}
