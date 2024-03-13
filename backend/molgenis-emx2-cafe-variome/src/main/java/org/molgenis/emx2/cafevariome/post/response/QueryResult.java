package org.molgenis.emx2.cafevariome.post.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryResult {

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String type;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private int count;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Payload payload;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private Source source;

  public String getType() {
    return type;
  }

  public int getCount() {
    return count;
  }

  public Payload getPayload() {
    return payload;
  }

  public Source getSource() {
    return source;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public void setPayload(Payload payload) {
    this.payload = payload;
  }

  public void setSource(Source source) {
    this.source = source;
  }
}
