package org.molgenis.emx2.beaconv2.endpoints.datasets;

import org.molgenis.emx2.beaconv2.common.misc.Info;
import org.molgenis.emx2.beaconv2.endpoints.OntologyTerm;

// https://github.com/ga4gh-beacon/beacon-v2-Models/blob/main/BEACON-V2-draft4-Model/datasets/defaultSchema.json
public class Dataset {
  // Unique identifier of the dataset
  private String id;
  // Name of the dataset
  private String name;
  // Description of the dataset
  private String description;
  // The time the dataset was created (ISO 8601 format)
  private String createDateTime;
  // The time the dataset was updated in (ISO 8601 format)
  private String updateDateTime;
  // Version of the dataset
  private String version;
  // URL to an external system providing more dataset information (RFC 3986 format).
  private String externalUrl;
  //
  private Info info;
  //
  private DataUseConditions dataUseConditions;

  private class DataUseConditions {
    DUODataUse[] duoDataUse;
  }

  private class DUODataUse {
    String version;
    OntologyTerm[] items;
  }
}
