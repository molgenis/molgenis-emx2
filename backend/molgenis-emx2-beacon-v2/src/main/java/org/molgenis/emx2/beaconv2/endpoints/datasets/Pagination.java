package org.molgenis.emx2.beaconv2.endpoints.datasets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Pagination {

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private int skip = 0;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private int limit = 10;
}
