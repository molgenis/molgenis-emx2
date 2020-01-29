package org.molgenis.emx2.utils;

import java.util.Collections;
import java.util.Map;

public class Role {
  public static enum Privilege {
    META,
    READ, // select
    ADD, // insert
    WRITE, // update
    DELETE, // delete and truncate
    GRANT
  }

  private String schema;
  private String name;
  private Map<String, Privilege[]> tablePermissions;

  Map<String, Privilege[]> getTablePermissions() {
    return Collections.unmodifiableMap(tablePermissions);
  }
}
