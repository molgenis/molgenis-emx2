package org.molgenis.emx2;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

  private static final Set<String> SYSTEM_ROLE_NAMES =
      Arrays.stream(values()).map(Privileges::toString).collect(Collectors.toUnmodifiableSet());

  private final String name;

  Privileges(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static boolean isSystemRole(String roleName) {
    return SYSTEM_ROLE_NAMES.contains(roleName);
  }
}
