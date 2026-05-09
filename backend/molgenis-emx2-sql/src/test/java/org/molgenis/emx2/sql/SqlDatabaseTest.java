package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.emx2.Constants;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class SqlDatabaseTest {
  @SystemStub private static EnvironmentVariables environmentVariables;
  static SqlDatabase sqlDatabase;

  @BeforeEach
  void setUp() {
    sqlDatabase = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void OIDCFlagDefaultsFalse() {
    sqlDatabase.removeSetting(Constants.IS_OIDC_ENABLED);
    assertFalse(sqlDatabase.isOidcEnabled());
  }

  @Test
  void enableOIDCFlagViaSettings() {
    environmentVariables.set(Constants.MOLGENIS_OIDC_CLIENT_ID, "id");
    environmentVariables.set(Constants.MOLGENIS_OIDC_CLIENT_SECRET, "ssst");
    sqlDatabase.setSetting(Constants.IS_OIDC_ENABLED, "true");
    assertTrue(sqlDatabase.isOidcEnabled());
    sqlDatabase.setSetting(Constants.IS_OIDC_ENABLED, "false");
    assertFalse(sqlDatabase.isOidcEnabled());
  }
}
