package org.molgenis.emx2;

public enum Privileges {
  // can select
  AGGREGATOR("Aggregator"),
  // can select
  VIEWER("Viewer"),
  // can insert, update, delete, implies Viewer
  EDITOR("Editor"),
  // extends Editor to create, alter, drop, implies Manager
  MANAGER("Manager"),
  // can add/remove users to schema
  OWNER("Owner");

  private String name;

  Privileges(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
