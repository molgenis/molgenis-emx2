package org.molgenis.emx2;

public class Member {
  private String user;
  private String role;

  public Member() {
    // used for automatic (de)serialization, in e.g. json
  }

  public Member(String user, String role) {
    this.user = user;
    this.role = role;
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
}
