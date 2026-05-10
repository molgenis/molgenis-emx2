package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_OWNER_COLUMN;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;

/**
 * Verifies the write-time FK-RLS visibility guard: INSERT and UPDATE must be rejected when the
 * new/updated row references a row in an RLS-enabled refTable that lies outside the writing user's
 * (view ∪ reference) scope. Tests mirror the read-path guard in SqlQuery.fkRlsVisibilityConditions.
 *
 * <p>Schema layout: REF_SCHEMA — "Animal" table, RLS enabled, rows owned by FkGuardAlice or
 * FkGuardBob. CHILD_SCHEMA — "Adoption" (REF → Animal) and "Shelter" (REF_ARRAY → Animal).
 */
class TestFkRlsWriteGuard {

  private static final String REF_SCHEMA = "FkRlsGuardRef";
  private static final String CHILD_SCHEMA = "FkRlsGuardChild";
  private static final String ANIMAL_TABLE = "Animal";
  private static final String ADOPTION_TABLE = "Adoption";
  private static final String SHELTER_TABLE = "Shelter";
  private static final String NON_RLS_TABLE = "Species";
  private static final String PLAIN_ADOPTION_TABLE = "PlainAdoption";

  private static final String ROLE_REF_NONE = "refNoneRole";
  private static final String ROLE_REF_ALL = "refAllRole";
  private static final String ROLE_VIEW_ALL = "viewAllRole";
  private static final String ROLE_CHANGE_OWNER_ONLY = "changeOwnerOnlyRole";
  private static final String GROUP_ALICE = "fkGuardGroupAlice";

  private static final String USER_ALICE = "FkGuardAlice";
  private static final String USER_BOB = "FkGuardBob";

  private static Database db;
  private static SqlRoleManager roleManager;
  private static Schema refSchema;
  private static Schema childSchema;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.becomeAdmin();
    roleManager = ((SqlDatabase) db).getRoleManager();

    Migrations.executeMigrationFile(
        db, "migration32.sql", "re-apply migration32 for mg_can_reference");

    db.dropSchemaIfExists(CHILD_SCHEMA);
    db.dropSchemaIfExists(REF_SCHEMA);

    refSchema = db.createSchema(REF_SCHEMA);
    childSchema = db.createSchema(CHILD_SCHEMA);

    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);

    // Animal table in refSchema — RLS enabled
    refSchema.create(table(ANIMAL_TABLE, column("name").setPkey(), column("kind")));
    refSchema.getTable(ANIMAL_TABLE).getMetadata().setRlsEnabled(true);
    refSchema
        .getTable(ANIMAL_TABLE)
        .insert(
            row("name", "fido", "kind", "dog").setString(MG_OWNER_COLUMN, USER_ALICE),
            row("name", "rex", "kind", "dog").setString(MG_OWNER_COLUMN, USER_BOB));

    // Adoption table in childSchema — scalar REF to Animal
    childSchema.create(
        table(
            ADOPTION_TABLE,
            column("id").setPkey(),
            column("notes"),
            column("animal").setType(REF).setRefSchemaName(REF_SCHEMA).setRefTable(ANIMAL_TABLE)));

    // Shelter table in childSchema — REF_ARRAY to Animal
    childSchema.create(
        table(
            SHELTER_TABLE,
            column("id").setPkey(),
            column("animals")
                .setType(REF_ARRAY)
                .setRefSchemaName(REF_SCHEMA)
                .setRefTable(ANIMAL_TABLE)));

    // Non-RLS reference table within CHILD_SCHEMA — same schema, no cross-schema access issue
    childSchema.create(table(NON_RLS_TABLE, column("name").setPkey(), column("class")));
    childSchema.getTable(NON_RLS_TABLE).insert(row("name", "canine", "class", "mammal"));

    // PlainAdoption table in childSchema — scalar REF to non-RLS within-schema Species table
    childSchema.create(
        table(
            PLAIN_ADOPTION_TABLE,
            column("id").setPkey(),
            column("species").setType(REF).setRefTable(NON_RLS_TABLE)));

    // Role with REFERENCE_NONE on Animal — cannot insert FK pointing anywhere
    roleManager.createRole(REF_SCHEMA, ROLE_REF_NONE);
    roleManager.setPermissions(
        refSchema,
        ROLE_REF_NONE,
        new PermissionSet()
            .putTable(
                ANIMAL_TABLE,
                new TablePermission(ANIMAL_TABLE)
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.NONE)));

    // Role with REFERENCE_ALL on Animal — can insert FK pointing to any row
    roleManager.createRole(REF_SCHEMA, ROLE_REF_ALL);
    roleManager.setPermissions(
        refSchema,
        ROLE_REF_ALL,
        new PermissionSet()
            .putTable(
                ANIMAL_TABLE,
                new TablePermission(ANIMAL_TABLE)
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.ALL)));

    // Role with VIEW_ALL on Animal (no explicit reference) — view scope covers reference
    roleManager.createRole(REF_SCHEMA, ROLE_VIEW_ALL);
    roleManager.setPermissions(
        refSchema,
        ROLE_VIEW_ALL,
        new PermissionSet()
            .putTable(
                ANIMAL_TABLE,
                new TablePermission(ANIMAL_TABLE)
                    .select(SelectScope.ALL)
                    .reference(ReferenceScope.NONE)));

    // Role with change_owner=true but no SELECT or REFERENCE scope — must NOT grant reference
    roleManager.createRole(REF_SCHEMA, ROLE_CHANGE_OWNER_ONLY);
    roleManager.setPermissions(
        refSchema,
        ROLE_CHANGE_OWNER_ONLY,
        new PermissionSet()
            .putTable(
                ANIMAL_TABLE,
                new TablePermission(ANIMAL_TABLE)
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.NONE))
            .setChangeOwner(true));

    roleManager.createGroup(refSchema, GROUP_ALICE);

    // Alice has Editor on childSchema so she can insert into child tables
    childSchema.addMember(USER_ALICE, "Editor");
    // Bob has Editor on childSchema so he can insert into child tables
    childSchema.addMember(USER_BOB, "Editor");
  }

  @Test
  void insert_throws_whenFkTargetOutsideReferenceScope() {
    db.becomeAdmin();
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    try {
      db.setActiveUser(USER_ALICE);
      assertThrows(
          MolgenisException.class,
          () ->
              childSchema
                  .getTable(ADOPTION_TABLE)
                  .insert(row("id", "adp-throw1", "animal", "fido")),
          "Insert with REFERENCE_NONE must be rejected");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    }
  }

  @Test
  void insert_succeeds_whenFkTargetWithinReferenceScope() {
    db.becomeAdmin();
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_ALL);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () -> childSchema.getTable(ADOPTION_TABLE).insert(row("id", "adp-ok1", "animal", "fido")),
          "Insert with REFERENCE_ALL must succeed");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_ALL);
    }
  }

  @Test
  void insert_succeeds_whenViewScopeOnRefTable() {
    db.becomeAdmin();
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_VIEW_ALL);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              childSchema.getTable(ADOPTION_TABLE).insert(row("id", "adp-view1", "animal", "fido")),
          "INSERT with VIEW_ALL alone must succeed — view scope covers reference");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_VIEW_ALL);
    }
  }

  @Test
  void update_throws_whenChangingFkToInvisibleTarget() {
    db.becomeAdmin();
    // Insert a seed row as admin so Alice can update it
    childSchema.getTable(ADOPTION_TABLE).insert(row("id", "adp-upd1", "animal", "fido"));
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    try {
      db.setActiveUser(USER_ALICE);
      assertThrows(
          MolgenisException.class,
          () -> childSchema.getTable(ADOPTION_TABLE).update(row("id", "adp-upd1", "animal", "rex")),
          "Update changing FK to invisible target must be rejected");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    }
  }

  @Test
  void update_skipsCheck_whenFkColumnNotChanged() {
    db.becomeAdmin();
    childSchema
        .getTable(ADOPTION_TABLE)
        .insert(row("id", "adp-skip1", "notes", "initial", "animal", "fido"));
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              childSchema
                  .getTable(ADOPTION_TABLE)
                  .update(row("id", "adp-skip1", "notes", "just a note")),
          "Update of non-FK column must succeed even with REFERENCE_NONE");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    }
  }

  @Test
  void refArray_throws_whenAnyElementInvisible() {
    db.becomeAdmin();
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    try {
      db.setActiveUser(USER_ALICE);
      assertThrows(
          MolgenisException.class,
          () ->
              childSchema
                  .getTable(SHELTER_TABLE)
                  .insert(row("id", "shlt-throw1", "animals", new String[] {"fido", "rex"})),
          "REF_ARRAY insert with any invisible element must be rejected fail-fast");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_NONE);
    }
  }

  @Test
  void refArray_succeeds_whenAllElementsVisible() {
    db.becomeAdmin();
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_ALL);
    try {
      db.setActiveUser(USER_ALICE);
      assertDoesNotThrow(
          () ->
              childSchema
                  .getTable(SHELTER_TABLE)
                  .insert(row("id", "shlt-ok1", "animals", new String[] {"fido", "rex"})),
          "REF_ARRAY insert must succeed when all elements within REFERENCE_ALL scope");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_REF_ALL);
    }
  }

  @Test
  void crossSchema_throws_whenFkTargetOutsideReferenceScope() {
    db.becomeAdmin();
    String crossUser = "FkGuardCrossUser";
    String crossGroup = "fkGuardCrossGroup";
    if (!db.hasUser(crossUser)) db.addUser(crossUser);
    childSchema.addMember(crossUser, "Editor");
    roleManager.createGroup(refSchema, crossGroup);
    roleManager.addGroupMembership(REF_SCHEMA, crossGroup, crossUser, ROLE_REF_NONE);
    try {
      db.setActiveUser(crossUser);
      assertThrows(
          MolgenisException.class,
          () ->
              childSchema
                  .getTable(ADOPTION_TABLE)
                  .insert(row("id", "adp-cross1", "animal", "fido")),
          "Cross-schema FK insert with REFERENCE_NONE in refSchema must be rejected by the write guard");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(REF_SCHEMA, crossGroup, crossUser, ROLE_REF_NONE);
    }
  }

  @Test
  void insert_throws_whenChangeOwnerTrueButNoReferenceOrViewScope() {
    db.becomeAdmin();
    String changeOwnerUser = "FkGuardChangeOwnerUser";
    String changeOwnerGroup = "fkGuardChangeOwnerGroup";
    if (!db.hasUser(changeOwnerUser)) db.addUser(changeOwnerUser);
    childSchema.addMember(changeOwnerUser, "Editor");
    roleManager.createGroup(refSchema, changeOwnerGroup);
    roleManager.addGroupMembership(
        REF_SCHEMA, changeOwnerGroup, changeOwnerUser, ROLE_CHANGE_OWNER_ONLY);
    try {
      db.setActiveUser(changeOwnerUser);
      assertThrows(
          MolgenisException.class,
          () ->
              childSchema
                  .getTable(ADOPTION_TABLE)
                  .insert(row("id", "adp-chown1", "animal", "fido")),
          "change_owner=true without SELECT or REFERENCE scope must not grant FK reference");
    } finally {
      db.becomeAdmin();
      roleManager.removeGroupMembership(
          REF_SCHEMA, changeOwnerGroup, changeOwnerUser, ROLE_CHANGE_OWNER_ONLY);
    }
  }

  @Test
  void admin_bypassesCheck() {
    db.becomeAdmin();
    assertDoesNotThrow(
        () -> childSchema.getTable(ADOPTION_TABLE).insert(row("id", "adp-admin1", "animal", "rex")),
        "Admin must bypass the FK-RLS write guard");
  }

  @Test
  void rlsDisabledRefTable_noCheck() {
    db.becomeAdmin();
    String noRlsUser = "FkGuardNoRlsUser";
    if (!db.hasUser(noRlsUser)) db.addUser(noRlsUser);
    childSchema.addMember(noRlsUser, "Editor");
    // noRlsUser has no membership in REF_SCHEMA — but Species is NOT RLS-enabled
    try {
      db.setActiveUser(noRlsUser);
      assertDoesNotThrow(
          () ->
              childSchema
                  .getTable(PLAIN_ADOPTION_TABLE)
                  .insert(row("id", "plnadp-ok1", "species", "canine")),
          "FK to non-RLS refTable must not trigger the write guard");
    } finally {
      db.becomeAdmin();
    }
  }
}
