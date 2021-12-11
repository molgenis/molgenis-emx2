package org.molgenis.emx2.beacon.common;

public class EntryType {
  String schema;
  String id;
  String name;
  String description;
  OntologyTerm ontologyTermForThisType;
  String partOfSpecification;
  ReferenceToAnSchema defaultSchema;
  ReferenceToAnSchema[] additionallySupportedSchemas;
  BasicElement[] aCollectionOfaCollectionOf;
  String filteringTerms;
  boolean nonFilteredQueriesAllowed;

  public static class BasicElement {
    String id;
    String name;
  }

  public static class ReferenceToAnSchema {
    String referenceToSchemaDefinition;
    String schemaVersion;
  }
}
