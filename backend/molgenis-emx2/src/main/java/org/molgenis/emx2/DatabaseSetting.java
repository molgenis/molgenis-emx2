package org.molgenis.emx2;

import static java.util.Objects.requireNonNull;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Database level settings that have a known meaning, each with the type of its value. Settings with
 * keys that are not defined here are still allowed, but are not validated.
 *
 * @param <T> the type of the setting when retrieved
 */
public final class DatabaseSetting<T> {
  private static final Map<String, DatabaseSetting<?>> BY_KEY = new LinkedHashMap<>();

  // The individual settings
  public static final DatabaseSetting<ZoneId> TIMEZONE =
      new DatabaseSetting<>("timezone", DatabaseSetting::parseZoneId, ZoneId::systemDefault);

  private final String key;
  private final Function<String, T> parser;
  private final Supplier<T> defaultValue;

  private DatabaseSetting(String key, Function<String, T> parser) {
    this(key, parser, null);
  }

  private DatabaseSetting(
      String key, Function<String, T> parser, @Nullable Supplier<T> defaultValue) {
    this.key = requireNonNull(key);
    this.parser = requireNonNull(parser);
    this.defaultValue = defaultValue;
    if (BY_KEY.putIfAbsent(key, this) != null) {
      throw new IllegalStateException("Duplicate database setting key: " + key);
    }
  }

  public String getKey() {
    return key;
  }

  public T getValue(Database database) {
    String value = requireNonNull(database).getSetting(key);
    return value == null ? defaultValue.get() : parse(value);
  }

  public void validate(@Nullable String value) {
    if (value != null) {
      parse(value);
    }
  }

  private T parse(String value) {
    try {
      return parser.apply(value);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid value for setting '" + key + "': '" + value + "'. " + e.getMessage(), e);
    }
  }

  public static Optional<DatabaseSetting<?>> fromKey(String key) {
    return Optional.ofNullable(BY_KEY.get(key));
  }

  public static void validate(Map<String, String> settings) {
    settings.forEach((key, value) -> fromKey(key).ifPresent(setting -> setting.validate(value)));
  }

  @Override
  public String toString() {
    return key;
  }

  // Setting specific code for validation/retrieval
  private static ZoneId parseZoneId(String value) {
    try {
      return ZoneId.of(value.trim());
    } catch (DateTimeException e) {
      throw new IllegalArgumentException(
          "Use a timezone ID such as 'Europe/Amsterdam', 'UTC' or '+01:00'.", e);
    }
  }
}
