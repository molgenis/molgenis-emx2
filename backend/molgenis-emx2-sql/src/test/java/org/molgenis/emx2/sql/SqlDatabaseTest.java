package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Maps;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.MolgenisException;
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
    Map<String, String> settings = Maps.newHashMap();
    sqlDatabase.setSettings(settings);
    assertFalse(sqlDatabase.isOidcEnabled());
  }

  @Test
  void enableOIDCFlagViaSettings() {
    environmentVariables.set(Constants.MOLGENIS_OIDC_CLIENT_ID, "id");
    environmentVariables.set(Constants.MOLGENIS_OIDC_CLIENT_SECRET, "ssst");
    Map<String, String> settings = Maps.newHashMap();
    settings.put(Constants.IS_OIDC_ENABLED, "true");
    sqlDatabase.setSettings(settings);
    assertTrue(sqlDatabase.isOidcEnabled());
    settings.put(Constants.IS_OIDC_ENABLED, "false");
    sqlDatabase.setSettings(settings);
    assertFalse(sqlDatabase.isOidcEnabled());
  }

  @Test
  void invalidOIDCSettings() {
    environmentVariables.set(Constants.MOLGENIS_OIDC_CLIENT_ID, null);
    environmentVariables.set(Constants.MOLGENIS_OIDC_CLIENT_SECRET, null);
    Map<String, String> settings = Maps.newHashMap();
    settings.put(Constants.IS_OIDC_ENABLED, "true");
    assertThrows(MolgenisException.class, () -> sqlDatabase.setSettings(settings));
  }
}
