package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.molgenis.emx2.*;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class SqlDatabaseTest {

  @SystemStub private static EnvironmentVariables env;

  private static final Database DATABASE = TestDatabaseFactory.getTestDatabase();

  @Test
  void OIDCFlagDefaultsFalse() {
    Map<String, String> settings = Maps.newHashMap();
    DATABASE.setSettings(settings);
    assertFalse(DATABASE.isOidcEnabled());
  }

  @Test
  void enableOIDCFlagViaSettings() {
    env.set(Constants.MOLGENIS_OIDC_CLIENT_ID, "id");
    env.set(Constants.MOLGENIS_OIDC_CLIENT_SECRET, "ssst");
    Map<String, String> settings = Maps.newHashMap();
    settings.put(Constants.IS_OIDC_ENABLED, "true");
    DATABASE.setSettings(settings);
    assertTrue(DATABASE.isOidcEnabled());
    settings.put(Constants.IS_OIDC_ENABLED, "false");
    DATABASE.setSettings(settings);
    assertFalse(DATABASE.isOidcEnabled());
  }

  @Test
  void whenSchemaMetadataDoesNotExist_thenThrowMolgenisException() {
    assertThrows(MolgenisException.class, () -> DATABASE.getSchemaMetadata("non-existent-schema"));
  }

  @Test
  void whenSchemaDoesNotExist_thenReturnNull() {
    assertNull(DATABASE.getSchema("non-existent-schema"));
  }

  @Test
  void givenUsername_thenUpdateRoles() {
    Username foo = new Username("foo");
    Schema schema = DATABASE.dropCreateSchema(SqlDatabase.class.getSimpleName());
    schema.addMember(foo, "Count");

    DATABASE.updateRoles(
        foo, List.of(Map.of("schemaId", schema.getName(), Constants.ROLE, "Editor")));
    assertEquals(List.of(new Member(foo, "Editor")), schema.getMembers());
  }
}
