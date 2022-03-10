package org.molgenis.emx2.beaconv2.responses.entrytypes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.CommonEntryTypes;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypesResponse {
  String $schema = "../../configuration/entryTypesSchema.json";
  CommonEntryTypes entryTypes = new CommonEntryTypes();
}
