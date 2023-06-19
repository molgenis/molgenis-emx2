package org.molgenis.emx2.cafevariome.response;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Source {

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String uid;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String name;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String display_name;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String description;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String owner_name;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String owner_email;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private String uri;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private long date_created;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private int record_count;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private boolean locked;

  @JsonInclude(JsonInclude.Include.ALWAYS)
  private boolean status;

  public void setUid(String uid) {
    this.uid = uid;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDisplay_name(String display_name) {
    this.display_name = display_name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setOwner_name(String owner_name) {
    this.owner_name = owner_name;
  }

  public void setOwner_email(String owner_email) {
    this.owner_email = owner_email;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setDate_created(long date_created) {
    this.date_created = date_created;
  }

  public void setRecord_count(int record_count) {
    this.record_count = record_count;
  }

  public void setLocked(boolean locked) {
    this.locked = locked;
  }

  public void setStatus(boolean status) {
    this.status = status;
  }
}
