package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.ISO8601duration;
import org.molgenis.emx2.beaconv2.common.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AgeOfOnset {
  private OntologyTerm ageGroup;
  private ISO8601duration age;

  public AgeOfOnset(OntologyTerm ageGroup, String age) {
    this.ageGroup = ageGroup;
    this.age = new ISO8601duration(age);
  }
}
