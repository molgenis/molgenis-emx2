package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Meta {

  private MetaRequest request;
  private String apiVersion;
  private Requires requires;
  private Query query;

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

  public Requires getRequires() {
    return requires;
  }

  public void setRequires(Requires requires) {
    this.requires = requires;
  }

  public Query getQuery() {
    return query;
  }

  public void setQuery(Query query) {
    this.query = query;
  }
}
