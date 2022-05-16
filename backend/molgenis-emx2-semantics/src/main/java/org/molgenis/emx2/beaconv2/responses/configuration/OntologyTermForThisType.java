package org.molgenis.emx2.beaconv2.responses.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OntologyTermForThisType {

  String id;
  String label;

  public OntologyTermForThisType(String id, String label) {
    this.id = id;
    this.label = label;
  }
}
