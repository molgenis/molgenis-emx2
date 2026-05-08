package org.molgenis.emx2;

import java.util.Locale;

public enum SelectScope {
  NONE,
  EXISTS,
  COUNT,
  RANGE,
  AGGREGATE,
  OWN,
  GROUP,
  ALL;

  public boolean allowsRowAccess() {
    return this == ALL || this == OWN || this == GROUP;
  }

  public boolean allowsCount() {
    return this == ALL
        || this == OWN
        || this == GROUP
        || this == COUNT
        || this == AGGREGATE
        || this == RANGE;
  }

  public boolean allowsMinMax() {
    return this == ALL || this == OWN || this == GROUP || this == AGGREGATE || this == RANGE;
  }

  public boolean allowsAvgSum() {
    return this == ALL || this == OWN || this == GROUP || this == AGGREGATE;
  }

  public boolean allowsGroupBy() {
    return this == ALL || this == OWN || this == GROUP || this == AGGREGATE;
  }

  public boolean allowsExactCount() {
    return this == ALL || this == OWN || this == GROUP;
  }

  public static SelectScope fromString(String name) {
    String upper = name.toUpperCase(Locale.ROOT);
    for (SelectScope scope : values()) {
      if (scope.name().equals(upper)) {
        return scope;
      }
    }
    throw new MolgenisException("Unknown SelectScope: " + name);
  }
}
