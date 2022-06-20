package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.endpoints.configuration.DefaultSchema;
import org.molgenis.emx2.beaconv2.endpoints.configuration.OntologyTermForThisType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class EntryType {

  private String id;
  private String name;
  private String partOfSpecification;
  private String description;
  private DefaultSchema defaultSchema;
  private OntologyTermForThisType ontologyTermForThisType;

  public EntryType(
      String entryTypeSingular,
      String entryTypePlural,
      String name,
      String description,
      String ontologyId,
      String ontologyLabel) {
    this.id = entryTypeSingular;
    this.name = name;
    this.partOfSpecification = "Beacon v2.0.0-draft.4";
    this.description = description;
    this.defaultSchema = new DefaultSchema(entryTypeSingular, entryTypePlural);
    this.ontologyTermForThisType = new OntologyTermForThisType(ontologyId, ontologyLabel);
  }

  // todo make richer? e.g.
  //  Map<String, String> aCollectionOf =
  //      new HashMap<String, String>() {
  //        {
  //          put("id", "genomicVariant");
  //          put("name", "Genomic Variants");
  //        }
  //      };

  /*
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
   */
}
