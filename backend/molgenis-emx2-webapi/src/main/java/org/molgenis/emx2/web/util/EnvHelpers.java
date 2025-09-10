package org.molgenis.emx2.web.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvHelpers {
  private static final Logger logger = LoggerFactory.getLogger(EnvHelpers.class);

  public static int getEnvInt(String name, Integer defaultValue) {
    String value = System.getenv(name);
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        logger.error(
            "Invalid integer for {}: '{}'. Using default: {}\n", name, value, defaultValue);
      }
    }
    return defaultValue;
  }

  public static long getEnvLong(String name, Long defaultValue) {
    String value = System.getenv(name);
    if (value != null) {
      try {
        return Long.parseLong(value);
      } catch (NumberFormatException e) {
        logger.error("Invalid long for {}: '{}'. Using default: {}\n", name, value, defaultValue);
      }
    }
    return defaultValue;
  }
}
