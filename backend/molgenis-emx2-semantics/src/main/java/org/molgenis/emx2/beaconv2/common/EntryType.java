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
}
