package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class RunMolgenisEmx2Test {

  private static final String[] NO_ARGUMENTS = {};
  private static final UnaryOperator<String> NOTHING_CONFIGURED = name -> null;

  private static UnaryOperator<String> configuredPort(String value) {
    return Map.of(Constants.MOLGENIS_HTTP_PORT, value)::get;
  }

  @Test
  void environmentValueIsHonouredWhenNoArgumentGiven() {
    assertEquals(8083, RunMolgenisEmx2.resolveHttpPort(NO_ARGUMENTS, configuredPort("8083")));
  }

  @Test
  void firstArgumentOverridesEnvironmentValue() {
    assertEquals(
        9090, RunMolgenisEmx2.resolveHttpPort(new String[] {"9090"}, configuredPort("8083")));
  }

  @Test
  void nonNumericArgumentFallsBackToEnvironmentValue() {
    assertEquals(
        8083, RunMolgenisEmx2.resolveHttpPort(new String[] {"notaport"}, configuredPort("8083")));
  }

  @Test
  void defaultsTo8080WhenNothingIsConfigured() {
    assertEquals(8080, RunMolgenisEmx2.resolveHttpPort(NO_ARGUMENTS, NOTHING_CONFIGURED));
  }

  @Test
  void defaultsTo8080WhenArgumentIsNonNumericAndNothingIsConfigured() {
    assertEquals(
        8080, RunMolgenisEmx2.resolveHttpPort(new String[] {"notaport"}, NOTHING_CONFIGURED));
  }

  @Test
  void nonNumericEnvironmentValueFailsStartup() {
    UnaryOperator<String> brokenConfiguration = configuredPort("notaport");
    assertThrows(
        MolgenisException.class,
        () -> RunMolgenisEmx2.resolveHttpPort(NO_ARGUMENTS, brokenConfiguration));
  }

  @Test
  void productionLookupReadsSystemProperty() {
    String previousPort = System.getProperty(Constants.MOLGENIS_HTTP_PORT);
    System.setProperty(Constants.MOLGENIS_HTTP_PORT, "8087");
    try {
      assertEquals(
          8087,
          RunMolgenisEmx2.resolveHttpPort(
              NO_ARGUMENTS, RunMolgenisEmx2::systemPropertyOrEnvironment));
    } finally {
      if (previousPort == null) {
        System.clearProperty(Constants.MOLGENIS_HTTP_PORT);
      } else {
        System.setProperty(Constants.MOLGENIS_HTTP_PORT, previousPort);
      }
    }
  }
}
