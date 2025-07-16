package org.molgenis.emx2.web.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class EnvHelpersTest {

  @Test
  void testGetEnvIntValid() throws Exception {
    new EnvironmentVariables("MY_ENV_VAR", "42")
        .execute(
            () -> {
              int value = EnvHelpers.getEnvInt("MY_ENV_VAR", 10);
              assertEquals(42, value);
            });
  }

  @Test
  void testGetEnvIntInvalid() throws Exception {
    new EnvironmentVariables("MY_ENV_VAR", "notanint")
        .execute(
            () -> {
              int value = EnvHelpers.getEnvInt("MY_ENV_VAR", 10);
              assertEquals(10, value); // should fall back to default
            });
  }

  @Test
  void testGetEnvIntMissing() {
    int value = EnvHelpers.getEnvInt("UNKNOWN_VAR", 123);
    assertEquals(123, value);
  }

  @Test
  void testGetEnvLongValid() throws Exception {
    new EnvironmentVariables("MY_LONG_VAR", "123456789")
        .execute(
            () -> {
              long value = EnvHelpers.getEnvLong("MY_LONG_VAR", 999L);
              assertEquals(123456789L, value);
            });
  }

  @Test
  void testGetEnvLongInvalid() throws Exception {
    new EnvironmentVariables("MY_LONG_VAR", "notalong")
        .execute(
            () -> {
              long value = EnvHelpers.getEnvLong("MY_LONG_VAR", 999L);
              assertEquals(999L, value); // should fall back
            });
  }

  @Test
  void testGetEnvLongMissing() {
    long value = EnvHelpers.getEnvLong("MISSING_LONG_VAR", 555L);
    assertEquals(555L, value);
  }
}
