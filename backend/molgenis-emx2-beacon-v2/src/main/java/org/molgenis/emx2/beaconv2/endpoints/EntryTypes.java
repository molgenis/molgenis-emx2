package org.molgenis.emx2.beaconv2.endpoints;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.Meta;
import org.molgenis.emx2.beaconv2.endpoints.entrytypes.EntryTypesResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypes {
  private Meta meta = new Meta("../beaconInfoResponse.json", "entry");
  private EntryTypesResponse response = new EntryTypesResponse();
}
