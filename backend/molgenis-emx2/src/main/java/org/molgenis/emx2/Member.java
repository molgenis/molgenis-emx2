package org.molgenis.emx2;

import java.util.Objects;

public class Member {
  private Username username;
  private String role;

  public Member() {
    // used for automatic (de)serialization, in e.g. json
  }

  public Member(Username username, String role) {
    this.username = username;
    this.role = role;
  }

  public Username getUsername() {
    return username;
  }

  public void setUsername(Username username) {
    this.username = username;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Member member = (Member) o;
    return Objects.equals(username, member.username) && Objects.equals(role, member.role);
  }

  @Override
  public int hashCode() {
    return Objects.hash(username, role);
  }

  @Override
  public String toString() {
    return "Member{" + "user='" + username + '\'' + ", role='" + role + '\'' + '}';
  }
}
