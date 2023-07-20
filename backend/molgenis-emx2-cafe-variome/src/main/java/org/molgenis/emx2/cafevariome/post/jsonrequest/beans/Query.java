package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Query {

  private QueryComponents components;

  public QueryComponents getComponents() {
    return components;
  }

  public void setComponents(QueryComponents components) {
    this.components = components;
  }

  @Override
  public String toString() {
    return "Query{" + "components=" + components + '}';
  }
}
