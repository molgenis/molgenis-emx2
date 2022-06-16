package org.molgenis.emx2.beaconv2.endpoints.cohorts;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.beaconv2.common.ISO8601duration;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class AgeRange {
  ISO8601duration start;
  ISO8601duration end;
}
