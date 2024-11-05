package org.molgenis.emx2;

public class Member {
  private String user;
  private String role;

  private String schema;

  public Member() {
    // used for automatic (de)serialization, in e.g. json
  }

  public Member(String user, String role) {
    this.user = user;
    this.role = role;
  }

  public Member(String user, String role, String schema) {
    this.user = user;
    this.role = role;
    this.schema = schema;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
  }
}
