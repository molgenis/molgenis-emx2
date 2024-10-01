package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RequiresResponseComponentsCollection {

  private String exists;
  private String count;

  public String getExists() {
    return exists;
  }

  public void setExists(String exists) {
    this.exists = exists;
  }

  public String getCount() {
    return count;
  }

  public void setCount(String count) {
    this.count = count;
  }

  @Override
  public String toString() {
    return "RequiresResponseComponentsCollection{"
        + "exists='"
        + exists
        + '\''
        + ", count='"
        + count
        + '\''
        + '}';
  }
}
