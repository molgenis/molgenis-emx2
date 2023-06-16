package org.molgenis.emx2.cafevariome.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Payload {

  private String[] subjects;
  private String[] attributes;

  public void setSubjects(String[] subjects) {
    this.subjects = subjects;
  }

  public void setAttributes(String[] attributes) {
    this.attributes = attributes;
  }
}
