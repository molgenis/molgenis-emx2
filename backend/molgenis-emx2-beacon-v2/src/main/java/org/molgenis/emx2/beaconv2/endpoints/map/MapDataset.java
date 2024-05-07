package org.molgenis.emx2.beaconv2.endpoints.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MapDataset {
  private String entryType;
  private String openAPIEndpointsDefinition;
  private String rootUrl;
  private String singleEntryUrl;
  private String filteringTermsUrl;

  public MapDataset(String entryType, String endPointUrl) {
    this.entryType = entryType;
    this.openAPIEndpointsDefinition = "./" + entryType + "/endpoints.json";
    this.rootUrl = endPointUrl;
    this.singleEntryUrl = this.rootUrl + "/{id}";
    this.filteringTermsUrl = this.singleEntryUrl + "/filtering_terms";
  }
}
