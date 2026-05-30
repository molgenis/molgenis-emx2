package org.molgenis.emx2;

import java.util.Objects;

public class Member {
  private String user;
  private String role;
  private String groupName;

  public Member() {
    // used for automatic (de)serialization, in e.g. json
  }

  public Member(String user, String role) {
    this.user = user;
    this.role = role;
  }

  public Member(String user, String role, String groupName) {
    this.user = user;
    this.role = role;
    this.groupName = groupName;
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

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Member member = (Member) o;
    return Objects.equals(user, member.user)
        && Objects.equals(role, member.role)
        && Objects.equals(groupName, member.groupName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, role, groupName);
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
        + ", groupName='"
        + groupName
        + '\''
        + '}';
  }
}
