package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaComponentsQueryIdentification {

  private String queryID;
  private String queryLabel;

  public String getQueryID() {
    return queryID;
  }

  public void setQueryID(String queryID) {
    this.queryID = queryID;
  }

  public String getQueryLabel() {
    return queryLabel;
  }

  public void setQueryLabel(String queryLabel) {
    this.queryLabel = queryLabel;
  }

  @Override
  public String toString() {
    return "MetaComponentsQueryIdentification{"
        + "queryID='"
        + queryID
        + '\''
        + ", queryLabel='"
        + queryLabel
        + '\''
        + '}';
  }
}
