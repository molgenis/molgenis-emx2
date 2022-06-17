package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AgeOfOnset {
  OntologyTerm ageGroup;

  public AgeOfOnset(OntologyTerm ageGroup) {
    this.ageGroup = ageGroup;
  }
}
