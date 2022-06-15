package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ResponseSummary {
  boolean exists = true;
}
