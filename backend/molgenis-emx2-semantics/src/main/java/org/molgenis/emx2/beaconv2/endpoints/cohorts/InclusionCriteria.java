package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InclusionCriteria {
  AgeRange ageRange;

  public InclusionCriteria(String start, String end) {
    this.ageRange = new AgeRange();
    this.ageRange.start = new Start();
    this.ageRange.end = new End();
    this.ageRange.start.iso8601duration = start;
    this.ageRange.end.iso8601duration = end;
  }
}
