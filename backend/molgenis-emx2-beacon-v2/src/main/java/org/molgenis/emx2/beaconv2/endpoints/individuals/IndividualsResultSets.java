package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResultSets {

  private String id;
  private String type;
  private String setType;
  private Boolean exists;
  private Integer resultsCount;
  private IndividualsResultSetsItem[] results;

  public IndividualsResultSets(String id, IndividualsResultSetsItem[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "biosamples";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }

  public Integer getResultsCount() {
    return resultsCount;
  }

  public IndividualsResultSetsItem[] getResults() {
    return results;
  }

  public String getId() {
    return id;
  }
}
