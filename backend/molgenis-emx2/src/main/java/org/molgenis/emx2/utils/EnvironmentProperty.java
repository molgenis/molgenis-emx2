package org.molgenis.emx2.utils;

import org.molgenis.emx2.ColumnType;

public class EnvironmentProperty {
  public static Object getParameter(String param, Object defaultValue, ColumnType type) {
    try {
      if (System.getProperty(param) != null) {
        return TypeUtils.getTypedValue(System.getProperty(param), type);
      } else if (System.getenv(param) != null) {
        return TypeUtils.getTypedValue(System.getenv(param), type);
      } else {
        return TypeUtils.getTypedValue(defaultValue, type);
      }
    } catch (Exception e) {
      throw new RuntimeException("Startup failed: could not read property/env variable " + param);
    }
  }
}
