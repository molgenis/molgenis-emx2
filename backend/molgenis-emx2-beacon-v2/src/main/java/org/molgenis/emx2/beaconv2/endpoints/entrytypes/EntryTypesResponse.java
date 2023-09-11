package org.molgenis.emx2.beaconv2.endpoints.entrytypes;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.EntryTypes;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypesResponse {
  private String $schema = "../../configuration/entryTypesSchema.json";
  private EntryTypes entryTypes = new EntryTypes();
}
