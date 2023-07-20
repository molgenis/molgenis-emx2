package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Meta {

  private MetaRequest request;
  private String apiVersion;

  public MetaRequest getRequest() {
    return request;
  }

  public void setRequest(MetaRequest request) {
    this.request = request;
  }
}
