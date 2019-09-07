package org.molgenis.emx2;

public enum Permission {
  MEMBER("Member"),
  EDITOR("Editor"),
  MANAGER("Manager"),
  OWNER("Owner");

  private String name;

  Permission(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
