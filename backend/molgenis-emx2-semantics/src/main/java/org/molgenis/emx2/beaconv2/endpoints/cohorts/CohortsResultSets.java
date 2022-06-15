package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CohortsResultSets {

  String id;
  String type;
  String setType;
  Boolean exists;
  Integer resultsCount;
  CohortsResultSets[] results;

  public CohortsResultSets(String id, int resultsCount, CohortsResultSets[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "analyses";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }

  public CohortsResultSets() {
    super();
  }
}
