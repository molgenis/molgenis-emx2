package org.molgenis.emx2.beaconv2.endpoints.biosamples;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class BiosamplesResultSets {

  String id;
  String type;
  String setType;
  Boolean exists;
  Integer resultsCount;
  BiosamplesResultSetsItem[] results;

  public BiosamplesResultSets(String id, int resultsCount, BiosamplesResultSetsItem[] results) {
    this.id = id;
    this.type = "dataset";
    this.setType = "biosamples";
    this.exists = true;
    this.resultsCount = results.length;
    this.results = results;
  }

  public BiosamplesResultSets() {
    super();
  }
}
