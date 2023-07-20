package org.molgenis.emx2.cafevariome.post.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Payload {

  public Payload() {
    this.subjects = new String[] {};
    this.attributes = new String[] {};
  }

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String[] subjects;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String[] attributes;

  public void setSubjects(String[] subjects) {
    this.subjects = subjects;
  }

  public void setAttributes(String[] attributes) {
    this.attributes = attributes;
  }
}
