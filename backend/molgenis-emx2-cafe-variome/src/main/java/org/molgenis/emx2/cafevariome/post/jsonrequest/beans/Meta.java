package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Meta {

  private MetaRequest request;
  private String apiVersion;
  private MetaComponents components;

  public MetaRequest getRequest() {
    return request;
  }

  public void setRequest(MetaRequest request) {
    this.request = request;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public MetaComponents getComponents() {
    return components;
  }

  public void setComponents(MetaComponents components) {
    this.components = components;
  }

  @Override
  public String toString() {
    return "Meta{"
        + "request="
        + request
        + ", apiVersion='"
        + apiVersion
        + '\''
        + ", components="
        + components
        + '}';
  }
}
