package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Database level settings that have a known meaning. Settings with keys that are not defined here
 * are still allowed, but are not validated.
 */
public enum DatabaseSetting {
  TIMEZONE("timezone") {
    /** {@code null} (setting not present) is valid and means the server timezone is used. */
    @Override
    public void validate(@Nullable String value) {
      if (value != null) {
        parseZoneId(value);
      }
    }

    /**
     * @return the timezone defined by this setting, or the server default timezone if this setting
     *     is not present
     * @throws IllegalArgumentException if this setting has an invalid value
     */
    @SuppressWarnings("unchecked") // see DatabaseSetting#getValue(Database)
    @Override
    public ZoneId getValue(Database database) {
      requireNonNull(database);
      String value = database.getSetting(TIMEZONE.getKey());
      return value == null ? ZoneId.systemDefault() : parseZoneId(value);
    }

    private ZoneId parseZoneId(String value) {
      try {
        return ZoneId.of(value.trim());
      } catch (DateTimeException e) {
        throw new IllegalArgumentException(
            "Invalid value for setting '"
                + getKey()
                + "': '"
                + value
                + "'. Use a timezone ID such as 'Europe/Amsterdam', 'UTC' or '+01:00'.",
            e);
      }
    }
  };

  private static final Map<String, DatabaseSetting> BY_KEY =
      Arrays.stream(values())
          .collect(Collectors.toMap(DatabaseSetting::getKey, Function.identity()));

  private final String key;

  DatabaseSetting(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  /**
   * Returns the (typed) value of this setting for the given {@link Database}, see the
   * implementation of each setting for the returned type.
   *
   * <p>IMPORTANT: Java enums cannot have type parameters, so the type {@code T} is inferred from
   * where the result is assigned to and is NOT checked by the compiler. Assigning it to a different
   * type than the setting returns (e.g. {@code String s = TIMEZONE.getValue(db)}) compiles, but
   * throws a {@link ClassCastException} when run.
   */
  public abstract <T> T getValue(Database database);

  public void validate(@Nullable String value) {
    // no validation by default
  }

  public static Optional<DatabaseSetting> fromKey(String key) {
    return Optional.ofNullable(BY_KEY.get(key));
  }

  public static void validate(Map<String, String> settings) {
    settings.forEach(
        (key, value) -> {
          fromKey(key).ifPresent(setting -> setting.validate(value));
        });
  }
}
