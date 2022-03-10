package org.molgenis.emx2.beaconv2.responses.map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EndPointSets {
  MapDataset dataset = new MapDataset();
  // TODO: others e.g. genomic variant that follow the same structure as MapDataset
}
