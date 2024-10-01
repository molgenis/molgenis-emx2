package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Requires {

  private RequiresResponse response;

  public RequiresResponse getResponse() {
    return response;
  }

  public void setResponse(RequiresResponse response) {
    this.response = response;
  }

  @Override
  public String toString() {
    return "Requires{" + "response=" + response + '}';
  }
}
