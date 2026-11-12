package org.molgenis.emx2;

import java.util.Objects;

public class Member {
  private Username user;
  private String role;

  public Member() {
    // used for automatic (de)serialization, in e.g. json
  }

  public Member(Username user, String role) {
    this.user = user;
    this.role = role;
  }

  public Username getUser() {
    return user;
  }

  public void setUser(Username user) {
    this.user = user;
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
    return Objects.equals(user, member.user) && Objects.equals(role, member.role);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, role);
  }

  @Override
  public String toString() {
    return "Member{" + "user='" + user + '\'' + ", role='" + role + '\'' + '}';
  }
}
