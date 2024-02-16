package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.node.ArrayNode;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResultSets {

  private String id;
  private String type;
  private String setType;
  private Boolean exists;
  private Integer resultsCount;
  private ArrayNode results;

  public IndividualsResultSets(String id, ArrayNode results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "biosamples";
    this.exists = true;
    this.resultsCount = results.size();
    this.results = results;
  }

  public Integer getResultsCount() {
    return resultsCount;
  }

  public ArrayNode getResults() {
    return results;
  }

  public String getId() {
    return id;
  }
}
