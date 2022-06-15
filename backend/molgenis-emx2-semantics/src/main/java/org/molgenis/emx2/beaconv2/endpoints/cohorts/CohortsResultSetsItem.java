package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CohortsResultSetsItem {

  String id;

  public CohortsResultSetsItem(String id) {
    this.id = id;
  }

  public CohortsResultSetsItem() {
    super();
  }
}
