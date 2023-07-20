package org.molgenis.emx2.cafevariome.post.jsonrequest.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class MetaRequestComponents {

  private MetaRequestComponentsSearch search;

  public MetaRequestComponentsSearch getSearch() {
    return search;
  }

  public void setSearch(MetaRequestComponentsSearch search) {
    this.search = search;
  }

  @Override
  public String toString() {
    return "MetaRequestComponents{" + "search=" + search + '}';
  }
}
