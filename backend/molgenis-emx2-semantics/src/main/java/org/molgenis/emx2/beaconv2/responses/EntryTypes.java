package org.molgenis.emx2.beaconv2.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.CommonMeta;
import org.molgenis.emx2.beaconv2.responses.entrytypes.EntryTypesResponse;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypes {
  CommonMeta meta = new CommonMeta("../beaconInfoResponse.json", "entry");
  EntryTypesResponse response = new EntryTypesResponse();
}
