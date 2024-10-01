package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaRequest {

  private MetaRequestComponents components;

  public MetaRequestComponents getComponents() {
    return components;
  }

  public void setComponents(MetaRequestComponents components) {
    this.components = components;
  }

  @Override
  public String toString() {
    return "MetaRequest{" + "components=" + components + '}';
  }
}
