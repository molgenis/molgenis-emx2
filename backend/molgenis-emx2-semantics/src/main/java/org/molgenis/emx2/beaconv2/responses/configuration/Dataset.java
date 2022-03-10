package org.molgenis.emx2.beaconv2.responses.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Dataset {
  String id = "dataset";
  String name = "Dataset";
  String partOfSpecification = "Beacon v2.0.0-draft.4";
  String description =
      "A Dataset is a collection of records, like rows in a database or cards in a cardholder.";
  DefaultSchema defaultSchema = new DefaultSchema();
  Map<String, String> aCollectionOf =
      new HashMap<String, String>() {
        {
          put("id", "genomicVariant");
          put("name", "Genomic Variants");
        }
      };
  OntologyTermForThisType ontologyTermForThisType = new OntologyTermForThisType();
}
