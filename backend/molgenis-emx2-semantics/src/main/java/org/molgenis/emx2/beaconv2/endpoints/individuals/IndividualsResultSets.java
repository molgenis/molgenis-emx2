package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class IndividualsResultSets {

  String id;
  String type;
  String setType;
  Boolean exists;
  Integer resultsCount;
  IndividualsResultSetsItem[] results;

  public IndividualsResultSets(String id, int resultsCount, IndividualsResultSetsItem[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "biosamples";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }
}
