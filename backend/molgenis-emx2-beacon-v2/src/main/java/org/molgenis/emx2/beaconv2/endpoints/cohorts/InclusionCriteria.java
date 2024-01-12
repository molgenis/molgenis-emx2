package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.ISO8601duration;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InclusionCriteria {
  private AgeRange ageRange;

  public InclusionCriteria(String start, String end) {
    this.ageRange = new AgeRange();
    this.ageRange.setStart(new ISO8601duration(start));
    this.ageRange.setEnd(new ISO8601duration(end));
  }
}
