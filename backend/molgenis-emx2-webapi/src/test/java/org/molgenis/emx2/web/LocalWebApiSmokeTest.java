package org.molgenis.emx2.web;

import org.junit.jupiter.api.BeforeAll;
import org.molgenis.emx2.RunMolgenisEmx2;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;

public class LocalWebApiSmokeTest extends WebApiSmokeTests {

  private static final int PORT = 8081; // other than default so we can see effect

  @BeforeAll
  static void setupServerAndSession() throws Exception {
    // start web service for testing, including env variables
    new EnvironmentVariables(
            org.molgenis.emx2.Constants.MOLGENIS_METRICS_ENABLED, Boolean.TRUE.toString())
        .execute(
            () -> {
              RunMolgenisEmx2.main(new String[] {String.valueOf(PORT)});
            });

    setAdminSession();
  }
}
