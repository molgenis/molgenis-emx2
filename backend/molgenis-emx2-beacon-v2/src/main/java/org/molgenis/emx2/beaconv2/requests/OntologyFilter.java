package org.molgenis.emx2.beaconv2.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OntologyFilter extends Filter {
  boolean includeDescendantTerms;
  Similarity similarity;

  public OntologyFilter(String id, String operator, String value) {
    super(id, operator, value);
  }
}
