package org.molgenis.emx2.utils;

import static java.util.Objects.requireNonNull;
import static org.molgenis.emx2.Constants.SETTING_TIMEZONE;

import java.time.DateTimeException;
import java.time.ZoneId;
import org.molgenis.emx2.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeZoneUtils {
  private static final Logger logger = LoggerFactory.getLogger(TimeZoneUtils.class);

  private TimeZoneUtils() {
    // static utility class
  }

  /**
   * Returns the timezone to use for the given {@link Database}.
   *
   * @return the timezone defined by the {@link org.molgenis.emx2.Constants#SETTING_TIMEZONE}
   *     database setting if configured with a valid value, otherwise the server default timezone.
   */
  public static ZoneId getZoneId(Database database) {
    requireNonNull(database);
    String value = database.getSetting(SETTING_TIMEZONE);
    if (value == null) {
      return ZoneId.systemDefault();
    }
    try {
      return ZoneId.of(value.trim());
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("Invalid time zone: " + value, e);
    }
  }
}
