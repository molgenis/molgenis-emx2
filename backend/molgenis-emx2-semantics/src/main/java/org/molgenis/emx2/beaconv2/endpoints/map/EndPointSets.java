package org.molgenis.emx2.beaconv2.endpoints.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EndPointSets {

  @JsonIgnore private String serverURL;

  private MapDataset analyses;
  private MapDataset biosamples;
  private MapDataset cohorts;
  private MapDataset datasets;
  private MapDataset g_variants;
  private MapDataset individuals;
  private MapDataset runs;

  public EndPointSets(String serverURL) {
    this.serverURL = serverURL;
    this.analyses = new MapDataset("analyses", this.serverURL);
    this.biosamples = new MapDataset("biosamples", this.serverURL);
    this.cohorts = new MapDataset("cohorts", this.serverURL);
    this.datasets = new MapDataset("datasets", this.serverURL);
    this.g_variants = new MapDataset("g_variants", this.serverURL);
    this.individuals = new MapDataset("individuals", this.serverURL);
    this.runs = new MapDataset("runs", this.serverURL);
  }
}
