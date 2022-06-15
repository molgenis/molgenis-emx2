package org.molgenis.emx2.beaconv2.endpoints.entrytypes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.EntryTypes;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypesResponse {
  String $schema = "../../configuration/entryTypesSchema.json";
  EntryTypes entryTypes = new EntryTypes();
}
