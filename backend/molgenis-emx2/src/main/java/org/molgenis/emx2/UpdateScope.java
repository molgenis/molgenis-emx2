package org.molgenis.emx2;

import java.util.Locale;

public enum UpdateScope {
  NONE,
  OWN,
  GROUP,
  ALL;

  public static UpdateScope fromString(String name) {
    String upper = name.toUpperCase(Locale.ROOT);
    for (UpdateScope scope : values()) {
      if (scope.name().equals(upper)) {
        return scope;
      }
    }
    throw new MolgenisException("Unknown UpdateScope: " + name);
  }
}
