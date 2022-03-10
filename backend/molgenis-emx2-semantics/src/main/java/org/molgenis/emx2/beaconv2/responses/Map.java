package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.CommonMeta;
import org.molgenis.emx2.beaconv2.responses.map.MapResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Map {
  CommonMeta meta = new CommonMeta("../beaconMapResponse.json", "map");
  MapResponse response = new MapResponse();
}
