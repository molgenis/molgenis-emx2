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
  public String datamodels;
  public String profiles;
  public String dataPartOfModelFolder;
  public String optionalDemoDataFolder;
  public List<String> datamodelsList;
  public List<String> profilesList;
  public List<String> dataPartOfModelFolderList;
  public List<String> optionalDemoDataFolderList;

  @Override
  public String toString() {
    return "Profiles{"
        + "name='"
        + name
        + '\''
        + ", description='"
        + description
        + '\''
        + ", datamodelsList="
        + datamodelsList
        + ", profilesList="
        + profilesList
        + ", dataPartOfModelFolderList="
        + dataPartOfModelFolderList
        + ", optionalDemoDataFolderList="
        + optionalDemoDataFolderList
        + '}';
  }
}
