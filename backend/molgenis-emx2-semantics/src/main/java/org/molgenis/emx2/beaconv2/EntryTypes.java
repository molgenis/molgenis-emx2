package org.molgenis.emx2.beaconv2;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryTypes {
  String $schema = "../entryTypeDefinition.json";
  String id = "individual";
  String name = "Individual entry";

  OntologyTermForThisType ontologyTermForThisType = new OntologyTermForThisType();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class OntologyTermForThisType {
    String id = "DUO:0000004\"";
    String label = "General Research Use";
  }

  PartOfSpecification partOfSpecification = new PartOfSpecification();

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class PartOfSpecification {
    DefaultSchema defaultSchema = new DefaultSchema();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class DefaultSchema {
      String id = "bdfGenericEntryType-v2";
      String name = "Beacon v2 default schema for a basic element.";
      String referenceToSchemaDefinition =
          "https://raw.githubusercontent.com/ga4gh-beacon/specification-v2-default-schemas/master/default_dataset_schema.yaml";
    }
  }

  /*
  hmmm
   */

  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
  public class Properties {
    Meta meta = new Meta();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Meta {
      String description =
          "Information about the response that could be relevant for the Beacon client in order to interpret the results.";
      String $ref =
          "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/responses/sections/beaconInformationalResponseMeta.json";
    }

    Response response = new Response();

    @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
    public class Response {
      String description = "Returning the Beacon configuration.";
      String $ref =
          "https://raw.githubusercontent.com/ga4gh-beacon/beacon-framework-v2/main/configuration/beaconConfigurationSchema.json";
    }
  }
}
