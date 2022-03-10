package org.molgenis.emx2.beaconv2.responses.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MapDataset {
  String entryType = "dataset";
  String openAPIEndpointsDefinition = "./datasets/endpoints.json";
  String rootUrl = "https://beacon.cafevariome.org/datasets";
  String singleEntryUrl = "https://beacon.cafevariome.org/datasets/{id}";
  String filteringTermsUrl = "https://beacon.cafevariome.org/datasets/{id}/filtering_terms";
}
