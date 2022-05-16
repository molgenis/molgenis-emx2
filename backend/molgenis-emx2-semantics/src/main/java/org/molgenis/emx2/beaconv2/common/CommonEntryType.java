package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.responses.configuration.DefaultSchema;
import org.molgenis.emx2.beaconv2.responses.configuration.OntologyTermForThisType;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CommonEntryType {

  String id;
  String name;
  String partOfSpecification;
  String description;
  DefaultSchema defaultSchema;
  OntologyTermForThisType ontologyTermForThisType;

  public CommonEntryType(
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
}
