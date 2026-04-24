package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.name;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Role;

class MetadataUtilsRoleTest {

  private static final DSLContext jooq =
      ((SqlDatabase) TestDatabaseFactory.getTestDatabase()).getJooq();

  @BeforeEach
  void clearRole() {
    jooq.deleteFrom(org.jooq.impl.DSL.table(name(MetadataUtils.MOLGENIS, "role_metadata")))
        .execute();
  }

  @Test
  void saveRoleInsertsNewRow() {
    Role role = new Role("reader");
    role.setDescription("Read-only access");

    MetadataUtils.saveRole(jooq, role);

    Role found = MetadataUtils.getRole(jooq, "reader");
    assertNotNull(found);
    assertEquals("reader", found.getRoleName());
    assertEquals("*", found.getSchemaName());
    assertEquals("Read-only access", found.getDescription());
    assertFalse(found.isImmutable());
    assertEquals("active", found.getStatus());
  }

  @Test
  void saveRoleUpsertsOnConflict() {
    Role role = new Role("editor");
    role.setDescription("Original description");
    MetadataUtils.saveRole(jooq, role);
    Role saved = MetadataUtils.getRole(jooq, "editor");
    assertNotNull(saved);

    role.setDescription("Updated description");
    MetadataUtils.saveRole(jooq, role);

    Role updated = MetadataUtils.getRole(jooq, "editor");
    assertNotNull(updated);
    assertEquals("Updated description", updated.getDescription());
  }

  @Test
  void listRolesExcludesDeletedByDefault() {
    Role active = new Role("active-role");
    active.setStatus("active");
    MetadataUtils.saveRole(jooq, active);

    Role deleted = new Role("deleted-role");
    deleted.setStatus("deleted");
    MetadataUtils.saveRole(jooq, deleted);

    List<Role> roles = MetadataUtils.listRoles(jooq);
    assertEquals(1, roles.size());
    assertEquals("active-role", roles.get(0).getRoleName());
  }

  @Test
  void listRolesWithIncludeDeletedShowsBoth() {
    Role active = new Role("active-role");
    active.setStatus("active");
    MetadataUtils.saveRole(jooq, active);

    Role deleted = new Role("deleted-role");
    deleted.setStatus("deleted");
    MetadataUtils.saveRole(jooq, deleted);

    List<Role> roles = MetadataUtils.listRoles(jooq, true);
    assertEquals(2, roles.size());
  }

  @Test
  void deleteRoleRemovesRow() {
    Role role = new Role("temp-role");
    MetadataUtils.saveRole(jooq, role);
    assertNotNull(MetadataUtils.getRole(jooq, "temp-role"));

    MetadataUtils.deleteRole(jooq, "temp-role");

    assertNull(MetadataUtils.getRole(jooq, "temp-role"));
  }
}
