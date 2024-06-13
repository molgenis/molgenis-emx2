package org.molgenis.emx2;

public enum Privileges {
  // can only see if data exists on aggregate queries
  EXISTS("Exists"),
  // can only see a certain range of counts exists on aggregate queries
  RANGE("Range"),
  // can aggregate
  AGGREGATOR("Aggregator"),
  // can also see exact counts < 10
  COUNT("Count"),
  // can select
  VIEWER("Viewer"),
  // can insert, update, delete, implies Viewer
  EDITOR("Editor"),
  // extends Editor to create, alter, drop, implies Editor
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
