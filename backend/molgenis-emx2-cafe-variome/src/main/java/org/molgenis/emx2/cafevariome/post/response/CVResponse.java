package org.molgenis.emx2.cafevariome.post.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class CVResponse {

  public CVResponse() {}

  @JsonInclude(JsonInclude.Include.ALWAYS)
  Map<String, QueryResult> sources;

  public void setSources(Map<String, QueryResult> sources) {
    this.sources = sources;
  }
}
