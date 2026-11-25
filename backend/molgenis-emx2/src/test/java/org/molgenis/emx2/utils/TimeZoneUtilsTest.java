package org.molgenis.emx2.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.Constants.SETTING_TIMEZONE;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DefaultTimeZone;

@DefaultTimeZone("Europe/Amsterdam")
class TimeZoneUtilsTest {
  @Test
  void testTimeZoneIfNotDefined() {
    Database database = databaseWithTimeZone(null);
    assertEquals(ZoneId.of("Europe/Amsterdam"), TimeZoneUtils.getZoneId(database));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  ", "invalid", "Not/AZone"})
  void testTimeZoneWithDatabaseSetting(String value) {
    Database database = databaseWithTimeZone(value);
    assertThrows(IllegalArgumentException.class, () -> TimeZoneUtils.getZoneId(database));
  }

  @Test
  void testTimezoneOverridesServerDefault() {
    assertEquals(
        ZoneId.of("Pacific/Auckland"),
        TimeZoneUtils.getZoneId(databaseWithTimeZone(" Pacific/Auckland ")));
  }

  @Test
  void testOffsetOverridesServerDefault() {
    assertEquals(ZoneId.of("+05:30"), TimeZoneUtils.getZoneId(databaseWithTimeZone("+05:30")));
  }

  private static Database databaseWithTimeZone(String value) {
    Database database = mock(Database.class);
    when(database.getSetting(SETTING_TIMEZONE)).thenReturn(value);
    return database;
  }
}
