package org.molgenis.emx2.beaconv2.endpoints.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OntologyTermForThisType {

  private String id;
  private String label;

  public OntologyTermForThisType(String id, String label) {
    this.id = id;
    this.label = label;
  }
}
