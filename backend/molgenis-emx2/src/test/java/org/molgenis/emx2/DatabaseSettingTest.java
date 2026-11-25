package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DefaultTimeZone("Europe/Amsterdam")
class DatabaseSettingTest {
  @Test
  void testValidSettingsMap() {
    assertDoesNotThrow(
        () ->
            DatabaseSetting.validate(
                Map.of(DatabaseSetting.TIMEZONE.getKey(), "Europe/Amsterdam")));
  }

  @Test
  void testInvalidSettingsMap() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DatabaseSetting.validate(Map.of(DatabaseSetting.TIMEZONE.getKey(), "invalid")));
  }

  @Test
  void testUnknownSettingsMap() {
    assertDoesNotThrow(() -> DatabaseSetting.validate(Map.of("unknown-key", "not-validated")));
  }

  @Nested
  class Timezone {
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Europe/Amsterdam", " Pacific/Auckland ", "UTC", "+05:30"})
    void testValidTimeZones(String value) {
      assertDoesNotThrow(() -> DatabaseSetting.TIMEZONE.validate(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "invalid", "Not/AZone", "Europe/Amsterdm", "+25:00", "CET+1"})
    void testInvalidTimeZones(String value) {
      assertThrows(IllegalArgumentException.class, () -> DatabaseSetting.TIMEZONE.validate(value));
    }

    @Test
    void testZoneIdIfNotDefined() {
      assertEquals(
          ZoneId.of("Europe/Amsterdam"),
          DatabaseSetting.TIMEZONE.getValue(databaseWithTimeZone(null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "invalid", "Not/AZone"})
    void testZoneIdWithInvalidSetting(String value) {
      Database database = databaseWithTimeZone(value);
      assertThrows(
          IllegalArgumentException.class, () -> DatabaseSetting.TIMEZONE.getValue(database));
    }

    @Test
    void testZoneIdOverridesServerDefault() {
      assertEquals(
          ZoneId.of("Pacific/Auckland"),
          DatabaseSetting.TIMEZONE.getValue(databaseWithTimeZone(" Pacific/Auckland ")));
    }

    @Test
    void testOffsetOverridesServerDefault() {
      assertEquals(
          ZoneId.of("+05:30"), DatabaseSetting.TIMEZONE.getValue(databaseWithTimeZone("+05:30")));
    }

    private static Database databaseWithTimeZone(String value) {
      Database database = mock(Database.class);
      when(database.getSetting(DatabaseSetting.TIMEZONE.getKey())).thenReturn(value);
      return database;
    }
  }
}
