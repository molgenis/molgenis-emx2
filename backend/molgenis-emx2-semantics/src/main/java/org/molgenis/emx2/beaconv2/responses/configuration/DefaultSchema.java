package org.molgenis.emx2.beaconv2.responses.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class DefaultSchema {
  String id = "ga4gh-beacon-dataset-v2.0.0-draft.4";
  String name = "Default schema for datasets";
  String referenceToSchemaDefinition = "./datasets/defaultSchema.json";
  String schemaVersion = "v2.0.0-draft.4";
}
