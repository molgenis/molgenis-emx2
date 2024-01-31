package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class GenericResultSet {

  private String id;
  private String type;
  private String setType;
  private Boolean exists;
  private Integer resultsCount;
  private List<Map> results;

  public GenericResultSet(String id, List<Map> results) {
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

  public List<Map> getResults() {
    return results;
  }

  public String getId() {
    return id;
  }
}
