package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Schema;

class SqlSchemaMetadataTest {

  @Test
  void givenAdminUser_whenRequestingInheritedRoles_thenReturnAllPrivileges() {
    Database database = TestDatabaseFactory.getTestDatabase();
    Schema schema = database.dropCreateSchema(getClass().getSimpleName());
    database.becomeAdmin();

    List<String> expectedRoles =
        Arrays.stream(Privileges.values()).map(Privileges::toString).toList();
    assertEquals(expectedRoles, schema.getInheritedRolesForActiveUser());
  }
}
