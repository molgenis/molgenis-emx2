package org.molgenis.emx2.beaconv2.responses.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MapDataset {
  String entryType;
  String openAPIEndpointsDefinition;
  String rootUrl;
  String singleEntryUrl;
  String filteringTermsUrl;

  public MapDataset(String entryType, String serverURL) {
    this.entryType = entryType;
    this.openAPIEndpointsDefinition = "./" + entryType + "/endpoints.json";
    this.rootUrl = serverURL + "api/beacon/" + entryType;
    this.singleEntryUrl = this.rootUrl + "/{id}";
    this.filteringTermsUrl = this.singleEntryUrl + "/filtering_terms";
  }
}
