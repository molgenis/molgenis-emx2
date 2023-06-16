package org.molgenis.emx2.cafevariome.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Response {

  public Response() {}

  @JsonInclude(JsonInclude.Include.ALWAYS)
  Map<String, QueryResult> sources;

  public void setSources(Map<String, QueryResult> sources) {
    this.sources = sources;
  }
}
