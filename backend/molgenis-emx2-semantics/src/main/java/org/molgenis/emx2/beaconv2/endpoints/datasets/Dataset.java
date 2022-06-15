package org.molgenis.emx2.beaconv2.endpoints.datasets;

import org.molgenis.emx2.beaconv2.common.OntologyTerm;
import org.molgenis.emx2.beaconv2.common.misc.Info;

// https://github.com/ga4gh-beacon/beacon-v2-Models/blob/main/BEACON-V2-draft4-Model/datasets/defaultSchema.json
public class Dataset {
  // Unique identifier of the dataset
  String id;
  // Name of the dataset
  String name;
  // Description of the dataset
  String description;
  // The time the dataset was created (ISO 8601 format)
  String createDateTime;
  // The time the dataset was updated in (ISO 8601 format)
  String updateDateTime;
  // Version of the dataset
  String version;
  // URL to an external system providing more dataset information (RFC 3986 format).
  String externalUrl;
  //
  Info info;
  //
  DataUseConditions dataUseConditions;

  private class DataUseConditions {
    DUODataUse[] duoDataUse;
  }

  private class DUODataUse {
    String version;
    OntologyTerm[] items;
  }
}
