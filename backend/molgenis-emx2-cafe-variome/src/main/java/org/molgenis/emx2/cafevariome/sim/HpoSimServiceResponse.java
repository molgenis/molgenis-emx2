package org.molgenis.emx2.cafevariome.sim;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class HpoSimServiceResponse {
  private String id;
  private String[] similarIDs;

  public String getId() {
    return id;
  }

  public String[] getSimilarIDs() {
    return similarIDs;
  }
}
