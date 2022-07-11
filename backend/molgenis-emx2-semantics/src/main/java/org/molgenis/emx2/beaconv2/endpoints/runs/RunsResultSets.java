package org.molgenis.emx2.beaconv2.endpoints.runs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RunsResultSets {

  private String id;
  private String type;
  private String setType;
  private Boolean exists;
  private Integer resultsCount;
  private RunsResultSetsItem[] results;

  public RunsResultSets(String id, int resultsCount, RunsResultSetsItem[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "runs";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }
}
