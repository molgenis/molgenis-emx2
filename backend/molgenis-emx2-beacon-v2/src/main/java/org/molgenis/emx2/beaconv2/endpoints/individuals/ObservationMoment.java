package org.molgenis.emx2.beaconv2.endpoints.individuals;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.ISO8601duration;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ObservationMoment {
  private ISO8601duration age;

  public ObservationMoment(ISO8601duration age) {
    this.age = age;
  }
}
