package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.ISO8601duration;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AgeRange {
  private ISO8601duration start;
  private ISO8601duration end;

  public void setStart(ISO8601duration start) {
    this.start = start;
  }

  public void setEnd(ISO8601duration end) {
    this.end = end;
  }
}
