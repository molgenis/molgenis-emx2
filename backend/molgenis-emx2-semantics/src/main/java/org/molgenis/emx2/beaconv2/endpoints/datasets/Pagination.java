package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Pagination {

  // TODO default? get from request?
  int skip = 0;
  int limit = 10;
}
