package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Role;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.TablePermission;
import org.molgenis.emx2.TablePermission.SelectScope;
import org.molgenis.emx2.TablePermission.UpdateScope;

class BuiltinInstallTest {

  private static final String SCHEMA = "builtin_install_test";
  private static final String TABLE = "data_table";

  private static final SqlDatabase database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  private SqlRoleManager roleManager;

  @BeforeEach
  void setup() {
    database.becomeAdmin();
    roleManager = new SqlRoleManager(database);
    database.dropSchemaIfExists(SCHEMA);
    Schema schema = database.createSchema(SCHEMA);
    schema.create(table(TABLE).add(column("id").setPkey()));
  }

  @AfterEach
  void teardown() {
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA);
  }

  @ParameterizedTest
  @CsvSource({
    "Exists,    EXISTS",
    "Range,     RANGE",
    "Aggregator,AGGREGATE",
    "Count,     COUNT",
    "Viewer,    ALL"
  })
  void selectScopeMatchesBuiltin(String privilegeName, String expectedSelect) {
    String schemaRoleName = SCHEMA + "/" + privilegeName;
    PermissionSet ps = roleManager.getPermissions(schemaRoleName);
    TablePermission resolved = ps.resolveFor(SCHEMA, TABLE);
    SelectScope expected = SelectScope.valueOf(expectedSelect.trim());
    assertTrue(
        resolved.select().contains(expected),
        privilegeName
            + " role must expose select="
            + expectedSelect
            + " after schema create; got "
            + resolved.select());
  }

  @Test
  void editorHasFullReadWriteAccess() {
    String editorRoleName = SCHEMA + "/" + Privileges.EDITOR.toString();
    PermissionSet ps = roleManager.getPermissions(editorRoleName);
    TablePermission resolved = ps.resolveFor(SCHEMA, TABLE);
    assertTrue(
        resolved.select().contains(SelectScope.ALL),
        "Editor must have select=ALL; got " + resolved);
    assertEquals(
        UpdateScope.ALL, resolved.insert(), "Editor must have insert=ALL; got " + resolved);
    assertEquals(
        UpdateScope.ALL, resolved.update(), "Editor must have update=ALL; got " + resolved);
    assertEquals(
        UpdateScope.ALL, resolved.delete(), "Editor must have delete=ALL; got " + resolved);
  }

  @Test
  void listBuiltinsUniform() {
    List<Role> roles = roleManager.getRoles(SCHEMA);
    List<String> roleNames = roles.stream().map(Role::name).toList();
    for (Privileges builtin : Privileges.values()) {
      boolean present = roleNames.contains(builtin.toString());
      assertEquals(
          true,
          present,
          "Built-in role " + builtin + " missing from getRoles(" + SCHEMA + "); got: " + roleNames);
    }
  }
}
