package org.molgenis.emx2.beaconv2.common;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.semantics.OntologyTerm;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AgeAndAgeGroup {
  private OntologyTerm ageGroup;
  private ISO8601duration age;

  public AgeAndAgeGroup(OntologyTerm ageGroup, String age) {
    this.ageGroup = ageGroup;
    this.age = new ISO8601duration(age);
  }

  public OntologyTerm getAgeGroup() {
    return ageGroup;
  }

  public ISO8601duration getAge() {
    return age;
  }
}
