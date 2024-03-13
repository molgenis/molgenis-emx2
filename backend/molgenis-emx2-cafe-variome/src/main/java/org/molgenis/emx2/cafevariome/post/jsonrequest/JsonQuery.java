package org.molgenis.emx2.cafevariome.post.jsonrequest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.molgenis.emx2.cafevariome.post.jsonrequest.beans.Logic;
import org.molgenis.emx2.cafevariome.post.jsonrequest.beans.Meta;
import org.molgenis.emx2.cafevariome.post.jsonrequest.beans.Query;
import org.molgenis.emx2.cafevariome.post.jsonrequest.beans.Requires;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class JsonQuery {

  private Meta meta;
  private Requires requires;
  private Query query;
  private Logic logic;

  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
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

  public Logic getLogic() {
    return logic;
  }

  public void setLogic(Logic logic) {
    this.logic = logic;
  }

  @Override
  public String toString() {
    return "JsonQuery{"
        + "meta="
        + meta
        + ", requires="
        + requires
        + ", query="
        + query
        + ", logic="
        + logic
        + '}';
  }
}
