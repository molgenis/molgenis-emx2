package org.molgenis.emx2.sql;

import static org.jooq.impl.DSL.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.PermissionSet;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.SelectScope;

class SqlRoleManagerTest {

  private static final String SCHEMA_A = "SqlRoleManagerTestA";
  private static final String SCHEMA_B = "SqlRoleManagerTestB";

  private static final Database db = TestDatabaseFactory.getTestDatabase();
  private static final DSLContext jooq = ((SqlDatabase) db).getJooq();
  private static final SqlRoleManager roleManager = new SqlRoleManager((SqlDatabase) db);

  private static final String TEST_USER_ALICE = "SqlRoleManagerTestAlice";
  private static final String TEST_USER_BOB = "SqlRoleManagerTestBob";

  private Schema schemaA;
  private Schema schemaB;

  @BeforeEach
  void setUp() {
    db.becomeAdmin();
    schemaA = db.dropCreateSchema(SCHEMA_A);
    schemaB = db.dropCreateSchema(SCHEMA_B);
    if (!db.hasUser(TEST_USER_ALICE)) db.addUser(TEST_USER_ALICE);
    if (!db.hasUser(TEST_USER_BOB)) db.addUser(TEST_USER_BOB);
  }

  @AfterEach
  void tearDown() {
    db.becomeAdmin();
    dropCustomRolesForSchema(SCHEMA_A);
    dropCustomRolesForSchema(SCHEMA_B);
    db.dropSchemaIfExists(SCHEMA_A);
    db.dropSchemaIfExists(SCHEMA_B);
  }

  private void dropCustomRolesForSchema(String schemaName) {
    String prefix = "MG_ROLE_" + schemaName + "/";
    List<String> toClean =
        jooq
            .fetch("SELECT rolname FROM pg_roles WHERE rolname LIKE {0}", inline(prefix + "%"))
            .stream()
            .map(r -> r.get(0, String.class))
            .filter(rolName -> !roleManager.isSystemRole(rolName.substring(prefix.length())))
            .toList();
    for (String rolName : toClean) {
      try {
        jooq.execute("DROP OWNED BY {0}", name(rolName));
      } catch (Exception ignored) {
      }
      try {
        jooq.execute("DROP ROLE IF EXISTS {0}", name(rolName));
      } catch (Exception ignored) {
      }
    }
  }

  @Test
  void createRole_persistsPgRoleWithSchemaPrefix() {
    roleManager.createRole(schemaA, "admin", "test admin role");

    boolean exists =
        jooq.fetchExists(
            jooq.select()
                .from("pg_roles")
                .where(field("rolname").eq(inline("MG_ROLE_" + SCHEMA_A + "/admin"))));
    assertTrue(exists, "pg_roles must contain MG_ROLE_<schema>/admin after createRole");
  }

  @Test
  void createRole_setsEmptyJsonComment() {
    roleManager.createRole(schemaA, "analyst", "test analyst role");

    String comment =
        jooq.fetchOne(
                "SELECT d.description FROM pg_authid a "
                    + "JOIN pg_shdescription d ON d.objoid = a.oid AND d.classoid = 'pg_authid'::regclass "
                    + "WHERE a.rolname = {0}",
                inline("MG_ROLE_" + SCHEMA_A + "/analyst"))
            .get(0, String.class);
    assertEquals("{}", comment, "COMMENT ON ROLE must be set to empty JSON object {}");
  }

  @Test
  void createRole_rejectsDuplicate() {
    roleManager.createRole(schemaA, "editor", "first");
    assertThrows(
        MolgenisException.class,
        () -> roleManager.createRole(schemaA, "editor", "second"),
        "duplicate role name must throw MolgenisException");
  }

  @Test
  void createRole_rejectsReservedPrefix() {
    assertThrows(
        MolgenisException.class,
        () -> roleManager.createRole(schemaA, "MG_custom", "reserved prefix"),
        "name starting with MG_ must be rejected");
  }

  @Test
  void createRole_rejectsEmptyOrTooLongName() {
    assertThrows(
        MolgenisException.class,
        () -> roleManager.createRole(schemaA, "", "empty name"),
        "empty name must be rejected");

    String schemaPrefix = "MG_ROLE_" + SCHEMA_A + "/";
    int prefixBytes = schemaPrefix.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    String nameThatExceedsLimit = "a".repeat(64 - prefixBytes + 1);
    assertThrows(
        MolgenisException.class,
        () -> roleManager.createRole(schemaA, nameThatExceedsLimit, "too long"),
        "name exceeding PG 63-byte role-name limit must be rejected");
  }

  @Test
  void deleteRole_dropsPgRole() {
    roleManager.createRole(schemaA, "todelete", "will be deleted");
    roleManager.deleteRole(schemaA, "todelete");

    boolean exists =
        jooq.fetchExists(
            jooq.select()
                .from("pg_roles")
                .where(field("rolname").eq(inline("MG_ROLE_" + SCHEMA_A + "/todelete"))));
    assertFalse(exists, "pg_roles must not contain the role after deleteRole");
  }

  @Test
  void deleteRole_rejectsMissing() {
    assertThrows(
        MolgenisException.class,
        () -> roleManager.deleteRole(schemaA, "nonexistent"),
        "deleting a non-existent role must throw MolgenisException");
  }

  @Test
  void listRoles_returnsCreatedRolesStrippedOfPrefix() {
    roleManager.createRole(schemaA, "role-one", "first");
    roleManager.createRole(schemaA, "role-two", "second");
    roleManager.createRole(schemaA, "role-three", "third");

    List<String> names = roleManager.listRoles(schemaA);

    assertTrue(names.contains("role-one"), "listRoles must include role-one");
    assertTrue(names.contains("role-two"), "listRoles must include role-two");
    assertTrue(names.contains("role-three"), "listRoles must include role-three");
    assertEquals(3, names.size(), "listRoles must return exactly 3 roles");
  }

  @Test
  void listRoles_isolatedPerSchema() {
    roleManager.createRole(schemaA, "isolated-role", "only in A");

    List<String> rolesB = roleManager.listRoles(schemaB);

    assertFalse(
        rolesB.contains("isolated-role"),
        "role created in schema A must not appear in listRoles(schemaB)");
  }

  @Test
  void grantRoleToUser_addsMembership() {
    roleManager.createRole(schemaA, "analyst", "test role");

    roleManager.grantRoleToUser(schemaA, "analyst", TEST_USER_ALICE);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "analyst");
    String fullUser = Constants.MG_USER_PREFIX + TEST_USER_ALICE;
    boolean isMember =
        jooq.fetchExists(
            jooq.select()
                .from("pg_auth_members am")
                .join("pg_roles r")
                .on("r.oid = am.roleid")
                .join("pg_roles m")
                .on("m.oid = am.member")
                .where(field("r.rolname").eq(inline(fullRole)))
                .and(field("m.rolname").eq(inline(fullUser))));
    assertTrue(isMember, "pg_auth_members must contain (role, user) after grantRoleToUser");
  }

  @Test
  void grantRoleToUser_rejectsSecondCustomRoleSameSchema() {
    roleManager.createRole(schemaA, "roleA", "first custom role");
    roleManager.createRole(schemaA, "roleB", "second custom role");
    roleManager.grantRoleToUser(schemaA, "roleA", TEST_USER_ALICE);

    MolgenisException thrown =
        assertThrows(
            MolgenisException.class,
            () -> roleManager.grantRoleToUser(schemaA, "roleB", TEST_USER_ALICE),
            "granting a second custom role in same schema must throw");
    assertTrue(
        thrown.getMessage().toLowerCase().contains("one custom role per schema"),
        "exception message must mention 'one custom role per schema'");

    String fullRoleB = SqlRoleManager.fullRoleName(SCHEMA_A, "roleB");
    String fullUser = Constants.MG_USER_PREFIX + TEST_USER_ALICE;
    boolean isMemberOfB =
        jooq.fetchExists(
            jooq.select()
                .from("pg_auth_members am")
                .join("pg_roles r")
                .on("r.oid = am.roleid")
                .join("pg_roles m")
                .on("m.oid = am.member")
                .where(field("r.rolname").eq(inline(fullRoleB)))
                .and(field("m.rolname").eq(inline(fullUser))));
    assertFalse(isMemberOfB, "pg_auth_members must not contain roleB membership after rejection");
  }

  @Test
  void grantRoleToUser_allowsSecondCustomRoleInDifferentSchema() {
    roleManager.createRole(schemaA, "roleInA", "role in schema A");
    roleManager.createRole(schemaB, "roleInB", "role in schema B");

    roleManager.grantRoleToUser(schemaA, "roleInA", TEST_USER_ALICE);
    assertDoesNotThrow(
        () -> roleManager.grantRoleToUser(schemaB, "roleInB", TEST_USER_ALICE),
        "granting custom roles in different schemas must be allowed");
  }

  @Test
  void grantRoleToUser_systemRoleExemptFromExclusivity() {
    roleManager.createRole(schemaA, "custom", "custom role");
    schemaA.addMember(TEST_USER_ALICE, "Editor");

    assertDoesNotThrow(
        () -> roleManager.grantRoleToUser(schemaA, "custom", TEST_USER_ALICE),
        "user holding a system role must also be granted a custom role without exclusivity violation");
  }

  @Test
  void grantRoleToUser_rejectsMissingRole() {
    assertThrows(
        MolgenisException.class,
        () -> roleManager.grantRoleToUser(schemaA, "nonexistent", TEST_USER_ALICE),
        "granting a non-existent role must throw MolgenisException");
  }

  @Test
  void grantRoleToUser_rejectsMissingUser() {
    roleManager.createRole(schemaA, "validrole", "test role");

    assertThrows(
        MolgenisException.class,
        () -> roleManager.grantRoleToUser(schemaA, "validrole", "ghost_user_does_not_exist"),
        "granting to a non-existent user must throw MolgenisException");
  }

  @Test
  void revokeRoleFromUser_removesMembership() {
    roleManager.createRole(schemaA, "analyst", "test role");
    roleManager.grantRoleToUser(schemaA, "analyst", TEST_USER_ALICE);

    roleManager.revokeRoleFromUser(schemaA, "analyst", TEST_USER_ALICE);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "analyst");
    String fullUser = Constants.MG_USER_PREFIX + TEST_USER_ALICE;
    boolean isMember =
        jooq.fetchExists(
            jooq.select()
                .from("pg_auth_members am")
                .join("pg_roles r")
                .on("r.oid = am.roleid")
                .join("pg_roles m")
                .on("m.oid = am.member")
                .where(field("r.rolname").eq(inline(fullRole)))
                .and(field("m.rolname").eq(inline(fullUser))));
    assertFalse(isMember, "pg_auth_members must not contain the pair after revokeRoleFromUser");
  }

  @Test
  void revokeRoleFromUser_rejectsNonMember() {
    roleManager.createRole(schemaA, "analyst", "test role");

    assertThrows(
        MolgenisException.class,
        () -> roleManager.revokeRoleFromUser(schemaA, "analyst", TEST_USER_BOB),
        "revoking from a user who was never granted the role must throw MolgenisException");
  }

  @Test
  void setPermissions_emptyRoundTrip() {
    roleManager.createRole(schemaA, "roundtrip", "empty round-trip role");
    PermissionSet empty = new PermissionSet();

    roleManager.setPermissions(schemaA, "roundtrip", empty);
    PermissionSet result = roleManager.getPermissions(schemaA, "roundtrip");

    assertEquals(empty, result, "empty PermissionSet must round-trip unchanged");
  }

  @Test
  void setPermissions_withTableScopesRoundTrip() {
    roleManager.createRole(schemaA, "scoped", "scoped role");
    PermissionSet permissions = new PermissionSet();
    permissions.putTable(
        "pet_store_x",
        new PermissionSet.TablePermissions()
            .setSelect(SelectScope.ALL)
            .setInsert(SelectScope.OWN)
            .setUpdate(SelectScope.GROUP)
            .setDelete(SelectScope.NONE));

    roleManager.setPermissions(schemaA, "scoped", permissions);
    PermissionSet result = roleManager.getPermissions(schemaA, "scoped");

    assertEquals(permissions, result, "PermissionSet with table scopes must round-trip unchanged");
    PermissionSet.TablePermissions tableResult = result.getTables().get("pet_store_x");
    assertEquals(SelectScope.ALL, tableResult.getSelect());
    assertEquals(SelectScope.OWN, tableResult.getInsert());
    assertEquals(SelectScope.GROUP, tableResult.getUpdate());
    assertEquals(SelectScope.NONE, tableResult.getDelete());
  }

  @Test
  void setPermissions_withFlagsRoundTrip() {
    roleManager.createRole(schemaA, "flagged", "flags role");
    PermissionSet permissions = new PermissionSet().setChangeOwner(true).setChangeGroup(true);

    roleManager.setPermissions(schemaA, "flagged", permissions);
    PermissionSet result = roleManager.getPermissions(schemaA, "flagged");

    assertEquals(permissions, result, "PermissionSet with flags must round-trip unchanged");
    assertTrue(result.isChangeOwner(), "changeOwner must be true after round-trip");
    assertTrue(result.isChangeGroup(), "changeGroup must be true after round-trip");
  }

  @Test
  void setPermissions_overwritesPriorDoc() {
    roleManager.createRole(schemaA, "overwrite", "overwrite role");
    PermissionSet first = new PermissionSet().setChangeOwner(true);
    PermissionSet second =
        new PermissionSet()
            .putTable("tableX", new PermissionSet.TablePermissions().setSelect(SelectScope.GROUP));

    roleManager.setPermissions(schemaA, "overwrite", first);
    roleManager.setPermissions(schemaA, "overwrite", second);
    PermissionSet result = roleManager.getPermissions(schemaA, "overwrite");

    assertEquals(second, result, "second setPermissions must overwrite the first");
    assertFalse(result.isChangeOwner(), "changeOwner must be false after second setPermissions");
    assertEquals(SelectScope.GROUP, result.getTables().get("tableX").getSelect());
  }

  @Test
  void setPermissions_throwsOnUnknownRole() {
    assertThrows(
        MolgenisException.class,
        () -> roleManager.setPermissions(schemaA, "nonexistent", new PermissionSet()),
        "setPermissions on non-existent role must throw MolgenisException");
  }

  @Test
  void getPermissions_returnsEmptyForFreshRole() {
    roleManager.createRole(schemaA, "fresh", "fresh role with empty comment");

    PermissionSet result = roleManager.getPermissions(schemaA, "fresh");

    assertNotNull(result, "getPermissions must return a non-null PermissionSet");
    assertTrue(result.getTables().isEmpty(), "fresh role must have no table entries");
    assertFalse(result.isChangeOwner(), "fresh role must have changeOwner=false");
    assertFalse(result.isChangeGroup(), "fresh role must have changeGroup=false");
  }

  @Test
  void setPermissions_addsMgOwnerColumnOnFirstOwnScope() {
    schemaA.create(table("Samples", column("name").setPkey()));
    roleManager.createRole(schemaA, "researcher", "researcher role");
    PermissionSet perms =
        new PermissionSet()
            .putTable("Samples", new PermissionSet.TablePermissions().setSelect(SelectScope.OWN));

    roleManager.setPermissions(schemaA, "researcher", perms);

    assertTrue(
        columnExistsInDb(SCHEMA_A, "Samples", Constants.MG_OWNER_COLUMN),
        "mg_owner column must appear after first OWN scope is set");
  }

  @Test
  void setPermissions_dropsMgOwnerColumnWhenLastOwnRemoved() {
    schemaA.create(table("Patients", column("id").setPkey()));
    roleManager.createRole(schemaA, "doctor", "doctor role");
    PermissionSet withOwn =
        new PermissionSet()
            .putTable("Patients", new PermissionSet.TablePermissions().setSelect(SelectScope.OWN));
    roleManager.setPermissions(schemaA, "doctor", withOwn);
    assertTrue(
        columnExistsInDb(SCHEMA_A, "Patients", Constants.MG_OWNER_COLUMN),
        "mg_owner must exist after OWN scope added");

    PermissionSet withoutOwn =
        new PermissionSet()
            .putTable("Patients", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaA, "doctor", withoutOwn);

    assertFalse(
        columnExistsInDb(SCHEMA_A, "Patients", Constants.MG_OWNER_COLUMN),
        "mg_owner must be dropped when last OWN scope is removed");
  }

  @Test
  void setPermissions_keepsMgOwnerColumnWhenAnotherRoleStillNeedsIt() {
    schemaA.create(table("Records", column("key").setPkey()));
    roleManager.createRole(schemaA, "roleX", "role X");
    roleManager.createRole(schemaA, "roleY", "role Y");
    PermissionSet ownPerms =
        new PermissionSet()
            .putTable("Records", new PermissionSet.TablePermissions().setSelect(SelectScope.OWN));
    roleManager.setPermissions(schemaA, "roleX", ownPerms);
    roleManager.setPermissions(schemaA, "roleY", ownPerms);

    PermissionSet noOwnPerms =
        new PermissionSet()
            .putTable("Records", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaA, "roleX", noOwnPerms);

    assertTrue(
        columnExistsInDb(SCHEMA_A, "Records", Constants.MG_OWNER_COLUMN),
        "mg_owner must stay when another role still has OWN scope");
  }

  @Test
  void setPermissions_addsMgGroupsColumnOnFirstGroupScope() {
    schemaA.create(table("Items", column("id").setPkey()));
    roleManager.createRole(schemaA, "analyst", "analyst role");
    PermissionSet perms =
        new PermissionSet()
            .putTable("Items", new PermissionSet.TablePermissions().setSelect(SelectScope.GROUP));

    roleManager.setPermissions(schemaA, "analyst", perms);

    assertTrue(
        columnExistsInDb(SCHEMA_A, "Items", Constants.MG_GROUPS_COLUMN),
        "mg_groups column must appear after first GROUP scope is set");
  }

  @Test
  void setPermissions_dropsMgGroupsColumnWhenLastGroupRemoved() {
    schemaA.create(table("Files", column("id").setPkey()));
    roleManager.createRole(schemaA, "viewer", "viewer role");
    PermissionSet withGroup =
        new PermissionSet()
            .putTable("Files", new PermissionSet.TablePermissions().setSelect(SelectScope.GROUP));
    roleManager.setPermissions(schemaA, "viewer", withGroup);
    assertTrue(
        columnExistsInDb(SCHEMA_A, "Files", Constants.MG_GROUPS_COLUMN),
        "mg_groups must exist after GROUP scope added");

    PermissionSet withoutGroup =
        new PermissionSet()
            .putTable("Files", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaA, "viewer", withoutGroup);

    assertFalse(
        columnExistsInDb(SCHEMA_A, "Files", Constants.MG_GROUPS_COLUMN),
        "mg_groups must be dropped when last GROUP scope is removed");
  }

  @Test
  void setPermissions_mgOwnerHasBtreeIndex() {
    schemaA.create(table("Events", column("id").setPkey()));
    roleManager.createRole(schemaA, "organizer", "organizer role");
    PermissionSet perms =
        new PermissionSet()
            .putTable("Events", new PermissionSet.TablePermissions().setSelect(SelectScope.OWN));

    roleManager.setPermissions(schemaA, "organizer", perms);

    String expectedIndexName = "Events_" + Constants.MG_OWNER_COLUMN + "_btree";
    List<org.jooq.Record> indexes =
        jooq.fetch(
            "SELECT indexname FROM pg_indexes WHERE schemaname = ? AND tablename = ? AND indexname = ?",
            SCHEMA_A,
            "Events",
            expectedIndexName);
    assertFalse(
        indexes.isEmpty(), "btree index '" + expectedIndexName + "' must exist on Events.mg_owner");
  }

  @Test
  void setPermissions_mgGroupsHasGinIndex() {
    schemaA.create(table("Projects", column("id").setPkey()));
    roleManager.createRole(schemaA, "member", "member role");
    PermissionSet perms =
        new PermissionSet()
            .putTable(
                "Projects", new PermissionSet.TablePermissions().setSelect(SelectScope.GROUP));

    roleManager.setPermissions(schemaA, "member", perms);

    String expectedIndexName = "Projects_" + Constants.MG_GROUPS_COLUMN + "_gin";
    List<org.jooq.Record> indexes =
        jooq.fetch(
            "SELECT indexname FROM pg_indexes WHERE schemaname = ? AND tablename = ? AND indexname = ?",
            SCHEMA_A,
            "Projects",
            expectedIndexName);
    assertFalse(
        indexes.isEmpty(),
        "GIN index '" + expectedIndexName + "' must exist on Projects.mg_groups");
  }

  @Test
  void setPermissions_mgOwnerDefaultedToInsertedByOnInsert() {
    schemaA.create(table("Observations", column("name").setPkey()));
    roleManager.createRole(schemaA, "collector", "collector role");
    PermissionSet perms =
        new PermissionSet()
            .putTable(
                "Observations", new PermissionSet.TablePermissions().setSelect(SelectScope.OWN));
    roleManager.setPermissions(schemaA, "collector", perms);

    schemaA.getTable("Observations").insert(new Row().setString("name", "obs-1"));

    String mgOwnerValue =
        jooq.fetchOne(
                "SELECT \"mg_owner\" FROM \""
                    + SCHEMA_A
                    + "\".\"Observations\" WHERE name = 'obs-1'")
            .get(0, String.class);
    assertNotNull(mgOwnerValue, "mg_owner must not be null after insert");
    assertFalse(mgOwnerValue.isEmpty(), "mg_owner must not be empty after insert");
  }

  @Test
  void setPermissions_concurrentCallsDoNotDropNeededColumn() throws Exception {
    schemaA.create(
        org.molgenis.emx2.TableMetadata.table("ConcurrentTable", column("id").setPkey()));
    roleManager.createRole(schemaA, "concRoleA", "concurrent role A");
    roleManager.createRole(schemaA, "concRoleB", "concurrent role B");
    PermissionSet ownPerms =
        new PermissionSet()
            .putTable(
                "ConcurrentTable", new PermissionSet.TablePermissions().setSelect(SelectScope.OWN));
    roleManager.setPermissions(schemaA, "concRoleA", ownPerms);

    PermissionSet noOwnPerms =
        new PermissionSet()
            .putTable(
                "ConcurrentTable", new PermissionSet.TablePermissions().setSelect(SelectScope.ALL));

    CountDownLatch startLatch = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<?> threadA =
          executor.submit(
              () -> {
                try {
                  startLatch.await();
                  roleManager.setPermissions(schemaA, "concRoleA", noOwnPerms);
                } catch (InterruptedException interruptedException) {
                  Thread.currentThread().interrupt();
                }
              });
      Future<?> threadB =
          executor.submit(
              () -> {
                try {
                  startLatch.await();
                  roleManager.setPermissions(schemaA, "concRoleB", ownPerms);
                } catch (InterruptedException interruptedException) {
                  Thread.currentThread().interrupt();
                }
              });
      startLatch.countDown();
      threadA.get();
      threadB.get();
    } finally {
      executor.shutdown();
    }

    assertTrue(
        columnExistsInDb(SCHEMA_A, "ConcurrentTable", Constants.MG_OWNER_COLUMN),
        "mg_owner must be present after concurrent setPermissions: roleB still needs OWN scope");
  }

  @Test
  void setPermissions_grantsInsertUpdateOnPermittedColumnsExcludingMgOwnerByDefault() {
    schemaA.create(table("Animals", column("name").setPkey(), column("species")));
    roleManager.createRole(schemaA, "curator", "curator role");
    PermissionSet perms =
        new PermissionSet()
            .putTable(
                "Animals",
                new PermissionSet.TablePermissions()
                    .setInsert(SelectScope.ALL)
                    .setUpdate(SelectScope.OWN));

    roleManager.setPermissions(schemaA, "curator", perms);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "curator");
    List<String> insertGrants = fetchColumnGrants(SCHEMA_A, "Animals", fullRole, "INSERT");
    List<String> updateGrants = fetchColumnGrants(SCHEMA_A, "Animals", fullRole, "UPDATE");

    assertTrue(insertGrants.contains("name"), "INSERT grant must include 'name' column");
    assertTrue(insertGrants.contains("species"), "INSERT grant must include 'species' column");
    assertFalse(
        insertGrants.contains(Constants.MG_OWNER_COLUMN),
        "INSERT grant must not include mg_owner by default");
    assertFalse(
        insertGrants.contains(Constants.MG_GROUPS_COLUMN),
        "INSERT grant must not include mg_groups by default");
    assertTrue(updateGrants.contains("name"), "UPDATE grant must include 'name' column");
    assertTrue(updateGrants.contains("species"), "UPDATE grant must include 'species' column");
    assertFalse(
        updateGrants.contains(Constants.MG_OWNER_COLUMN),
        "UPDATE grant must not include mg_owner by default");
  }

  @Test
  void setPermissions_changeOwnerTrueGrantsMgOwnerColumn() {
    schemaA.create(table("Docs", column("title").setPkey()));
    roleManager.createRole(schemaA, "doceditor", "doc editor role");
    PermissionSet perms =
        new PermissionSet()
            .setChangeOwner(true)
            .putTable(
                "Docs",
                new PermissionSet.TablePermissions()
                    .setInsert(SelectScope.ALL)
                    .setUpdate(SelectScope.OWN)
                    .setSelect(SelectScope.OWN));

    roleManager.setPermissions(schemaA, "doceditor", perms);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "doceditor");
    List<String> insertGrants = fetchColumnGrants(SCHEMA_A, "Docs", fullRole, "INSERT");
    List<String> updateGrants = fetchColumnGrants(SCHEMA_A, "Docs", fullRole, "UPDATE");

    assertTrue(
        insertGrants.contains(Constants.MG_OWNER_COLUMN),
        "INSERT grant must include mg_owner when changeOwner=true");
    assertTrue(
        updateGrants.contains(Constants.MG_OWNER_COLUMN),
        "UPDATE grant must include mg_owner when changeOwner=true");
  }

  @Test
  void setPermissions_changeGroupTrueGrantsMgGroupsColumn() {
    schemaA.create(table("Tasks", column("id").setPkey()));
    roleManager.createRole(schemaA, "taskowner", "task owner role");
    PermissionSet perms =
        new PermissionSet()
            .setChangeGroup(true)
            .putTable(
                "Tasks",
                new PermissionSet.TablePermissions()
                    .setInsert(SelectScope.ALL)
                    .setUpdate(SelectScope.GROUP)
                    .setSelect(SelectScope.GROUP));

    roleManager.setPermissions(schemaA, "taskowner", perms);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "taskowner");
    List<String> insertGrants = fetchColumnGrants(SCHEMA_A, "Tasks", fullRole, "INSERT");
    List<String> updateGrants = fetchColumnGrants(SCHEMA_A, "Tasks", fullRole, "UPDATE");

    assertTrue(
        insertGrants.contains(Constants.MG_GROUPS_COLUMN),
        "INSERT grant must include mg_groups when changeGroup=true");
    assertTrue(
        updateGrants.contains(Constants.MG_GROUPS_COLUMN),
        "UPDATE grant must include mg_groups when changeGroup=true");
  }

  @Test
  void setPermissions_flippingChangeOwnerFalseRevokesMgOwnerGrant() {
    schemaA.create(table("Reports", column("title").setPkey()));
    roleManager.createRole(schemaA, "reporter", "reporter role");
    PermissionSet withFlag =
        new PermissionSet()
            .setChangeOwner(true)
            .putTable(
                "Reports",
                new PermissionSet.TablePermissions()
                    .setInsert(SelectScope.ALL)
                    .setUpdate(SelectScope.OWN)
                    .setSelect(SelectScope.OWN));
    roleManager.setPermissions(schemaA, "reporter", withFlag);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "reporter");
    assertTrue(
        fetchColumnGrants(SCHEMA_A, "Reports", fullRole, "INSERT")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner INSERT grant must exist after changeOwner=true");

    PermissionSet withoutFlag =
        new PermissionSet()
            .setChangeOwner(false)
            .putTable(
                "Reports",
                new PermissionSet.TablePermissions()
                    .setInsert(SelectScope.ALL)
                    .setUpdate(SelectScope.OWN)
                    .setSelect(SelectScope.OWN));
    roleManager.setPermissions(schemaA, "reporter", withoutFlag);

    assertFalse(
        fetchColumnGrants(SCHEMA_A, "Reports", fullRole, "INSERT")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner INSERT grant must be revoked after changeOwner flips to false");
    assertFalse(
        fetchColumnGrants(SCHEMA_A, "Reports", fullRole, "UPDATE")
            .contains(Constants.MG_OWNER_COLUMN),
        "mg_owner UPDATE grant must be revoked after changeOwner flips to false");
  }

  @Test
  void setPermissions_noInsertScopeMeansNoColumnGrants() {
    schemaA.create(table("Logs", column("entry").setPkey()));
    roleManager.createRole(schemaA, "logviewer", "log viewer role");
    PermissionSet perms =
        new PermissionSet()
            .putTable(
                "Logs",
                new PermissionSet.TablePermissions()
                    .setInsert(SelectScope.NONE)
                    .setUpdate(SelectScope.ALL));

    roleManager.setPermissions(schemaA, "logviewer", perms);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "logviewer");
    List<String> insertGrants = fetchColumnGrants(SCHEMA_A, "Logs", fullRole, "INSERT");
    List<String> updateGrants = fetchColumnGrants(SCHEMA_A, "Logs", fullRole, "UPDATE");

    assertTrue(
        insertGrants.isEmpty(), "no INSERT column grants must exist when insert scope is NONE");
    assertFalse(
        updateGrants.isEmpty(), "UPDATE column grants must exist when update scope is non-NONE");
  }

  @Test
  void setPermissions_revokesGrantsWhenScopeFlipsToNone() {
    schemaA.create(table("Metrics", column("key").setPkey()));
    roleManager.createRole(schemaA, "analyst", "analyst role");
    PermissionSet withInsert =
        new PermissionSet()
            .putTable("Metrics", new PermissionSet.TablePermissions().setInsert(SelectScope.ALL));
    roleManager.setPermissions(schemaA, "analyst", withInsert);

    String fullRole = SqlRoleManager.fullRoleName(SCHEMA_A, "analyst");
    assertFalse(
        fetchColumnGrants(SCHEMA_A, "Metrics", fullRole, "INSERT").isEmpty(),
        "INSERT column grants must exist after INSERT=ALL");

    PermissionSet withoutInsert =
        new PermissionSet()
            .putTable("Metrics", new PermissionSet.TablePermissions().setInsert(SelectScope.NONE));
    roleManager.setPermissions(schemaA, "analyst", withoutInsert);

    assertTrue(
        fetchColumnGrants(SCHEMA_A, "Metrics", fullRole, "INSERT").isEmpty(),
        "INSERT column grants must be revoked after scope flips to NONE");
  }

  @Test
  @Disabled(
      "re-enable in Phase 4/5 once session-as-user fixture grants the custom role table-level INSERT — currently the custom role has no table-level INSERT grant (Phase 3), so the test would fail at the wrong layer")
  void setPermissions_sqlLevelRejectsMgOwnerUpdateWithoutFlag() {
    schemaA.create(table("Specimens", column("name").setPkey()));
    roleManager.createRole(schemaA, "collector", "collector role");
    PermissionSet perms =
        new PermissionSet()
            .putTable(
                "Specimens",
                new PermissionSet.TablePermissions()
                    .setInsert(SelectScope.ALL)
                    .setUpdate(SelectScope.ALL)
                    .setSelect(SelectScope.ALL));
    roleManager.setPermissions(schemaA, "collector", perms);
    schemaA.addMember(TEST_USER_ALICE, "Manager");
    schemaA.getTable("Specimens").insert(new Row().setString("name", "s-1"));

    db.setActiveUser(TEST_USER_ALICE);
    try {
      assertThrows(
          Exception.class,
          () ->
              schemaA
                  .getTable("Specimens")
                  .update(new Row().setString("name", "s-1").setString("mg_owner", "other")),
          "UPDATE setting mg_owner without changeOwner flag must throw permission denied");
    } finally {
      db.becomeAdmin();
    }
  }

  private List<String> fetchColumnGrants(
      String schemaName, String tableName, String fullRole, String verb) {
    return jooq
        .fetch(
            "SELECT column_name FROM information_schema.column_privileges "
                + "WHERE table_schema = {0} AND table_name = {1} "
                + "AND grantee = {2} AND privilege_type = {3}",
            inline(schemaName), inline(tableName), inline(fullRole), inline(verb))
        .stream()
        .map(r -> r.get("column_name", String.class))
        .toList();
  }

  private boolean columnExistsInDb(String schemaName, String tableName, String columnName) {
    return jooq.fetchExists(
        jooq.select()
            .from(name("information_schema", "columns"))
            .where(
                field(name("table_schema"))
                    .eq(inline(schemaName))
                    .and(field(name("table_name")).eq(inline(tableName)))
                    .and(field(name("column_name")).eq(inline(columnName)))));
  }
}
