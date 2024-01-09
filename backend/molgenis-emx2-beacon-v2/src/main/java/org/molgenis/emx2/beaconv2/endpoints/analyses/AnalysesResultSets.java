package org.molgenis.emx2.beaconv2.endpoints.analyses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AnalysesResultSets {

  private String id;
  private String type;
  private String setType;
  private Boolean exists;
  private Integer resultsCount;
  private AnalysesResultSetsItem[] results;

  public AnalysesResultSets(String id, int resultsCount, AnalysesResultSetsItem[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "analyses";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }
}
