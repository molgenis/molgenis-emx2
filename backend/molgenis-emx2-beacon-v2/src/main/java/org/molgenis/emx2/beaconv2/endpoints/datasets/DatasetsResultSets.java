package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DatasetsResultSets {

  private String id;
  private String type;
  private String setType;
  private Boolean exists;
  private Integer resultsCount;
  private DatasetsResultSetsItem[] results;

  public DatasetsResultSets(String id, DatasetsResultSetsItem[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "datasets";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }

  public Integer getResultsCount() {
    return resultsCount;
  }

  public DatasetsResultSetsItem[] getResults() {
    return results;
  }

  public String getId() {
    return id;
  }
}
