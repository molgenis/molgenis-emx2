package org.molgenis.emx2;

import java.util.Objects;

public class Member {
  private String user;
  private String role;
  private Boolean enabled;

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

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Member member = (Member) o;
    return Objects.equals(user, member.user) && Objects.equals(role, member.role);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, role);
  }

  @Override
  public String toString() {
    return "Member{"
        + "user='"
        + user
        + '\''
        + ", role='"
        + role
        + '\''
        + ", enabled="
        + enabled
        + '}';
  }
}
