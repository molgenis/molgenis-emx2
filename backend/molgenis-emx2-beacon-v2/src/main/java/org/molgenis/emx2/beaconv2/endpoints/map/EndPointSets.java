package org.molgenis.emx2.beaconv2.endpoints.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EndPointSets {

  @JsonIgnore private String serverURL;

  private MapDataset analysis;
  private MapDataset biosample;
  private MapDataset cohort;
  private MapDataset dataset;
  private MapDataset genomicVariant;
  private MapDataset individual;
  private MapDataset run;

  public EndPointSets(String serverURL) {
    this.serverURL = serverURL;
    String beaconUrl = this.serverURL + "api/beacon/";
    this.analysis = new MapDataset("analysis", beaconUrl + "analyses");
    this.biosample = new MapDataset("biosample", beaconUrl + "biosamples");
    this.cohort = new MapDataset("cohort", beaconUrl + "cohorts");
    this.dataset = new MapDataset("dataset", beaconUrl + "datasets");
    this.genomicVariant = new MapDataset("genomicVariant", beaconUrl + "g_variants");
    this.individual = new MapDataset("individual", beaconUrl + "individuals");
    this.run = new MapDataset("run", beaconUrl + "runs");
  }
}
