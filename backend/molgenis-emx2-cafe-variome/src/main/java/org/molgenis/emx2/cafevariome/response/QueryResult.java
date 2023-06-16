package org.molgenis.emx2.cafevariome.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class QueryResult {

  private String type;
  private int count;
  private Payload payload;
  private Source source;

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
