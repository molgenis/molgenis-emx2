package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RequiresResponseComponents {

  private RequiresResponseComponentsCollection collection;

  public RequiresResponseComponentsCollection getCollection() {
    return collection;
  }

  public void setCollection(RequiresResponseComponentsCollection collection) {
    this.collection = collection;
  }

  @Override
  public String toString() {
    return "RequiresResponseComponents{" + "collection=" + collection + '}';
  }
}
