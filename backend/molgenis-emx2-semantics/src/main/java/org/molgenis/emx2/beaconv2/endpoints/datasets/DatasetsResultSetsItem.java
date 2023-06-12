package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsResultSetsItem {

  private String id;
  private String name;
  private String createDateTime;

  // etc
  // todo
  // https://docs.genomebeacons.org/schemas-md/datasets_defaultSchema/

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCreateDateTime() {
    return createDateTime;
  }

  public void setCreateDateTime(String createDateTime) {
    this.createDateTime = createDateTime;
  }
}
