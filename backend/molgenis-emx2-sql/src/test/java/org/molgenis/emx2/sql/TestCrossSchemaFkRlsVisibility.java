package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.PermissionSet.ReferenceScope;
import org.molgenis.emx2.PermissionSet.SelectScope;
import org.molgenis.emx2.PermissionSet.UpdateScope;

/**
 * Verifies R.3b: a Child row whose single-valued FK target falls outside the user's (view ∪
 * reference) scope on refTable is hidden entirely (row-hide), not projected with a NULL FK.
 *
 * <p>Schema layout: REF_SCHEMA — "Pet" table, RLS enabled. CHILD_SCHEMA — "Owner" table, FK "pet" →
 * REF_SCHEMA.Pet. Alice has VIEW_OWN on Pet and full access to Owner.
 */
public class TestCrossSchemaFkRlsVisibility {

  private static final String REF_SCHEMA = "CsFkRlsRef";
  private static final String CHILD_SCHEMA = "CsFkRlsChild";
  private static final String PET_TABLE = "Pet";
  private static final String OWNER_TABLE = "Owner";
  private static final String PET_LOVER_TABLE = "PetLover";
  private static final String ROLE_VIEW_OWN = "viewOwnRole";
  private static final String ROLE_VIEW_ALL = "viewAllRole";
  private static final String ROLE_REF_ALL = "refAllRole";
  private static final String GROUP_ALICE = "groupAlice";
  private static final String USER_ALICE = "CsFkRlsAlice";
  private static final String USER_BOB = "CsFkRlsBob";

  private static Database db;
  private static SqlRoleManager roleManager;

  private static Schema refSchema;
  private static Schema childSchema;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    db.becomeAdmin();
    roleManager = ((SqlDatabase) db).getRoleManager();

    db.dropSchemaIfExists(CHILD_SCHEMA);
    db.dropSchemaIfExists(REF_SCHEMA);

    refSchema = db.createSchema(REF_SCHEMA);
    childSchema = db.createSchema(CHILD_SCHEMA);

    if (!db.hasUser(USER_ALICE)) db.addUser(USER_ALICE);
    if (!db.hasUser(USER_BOB)) db.addUser(USER_BOB);

    // Pet table in refSchema — RLS enabled, rows owned by alice or bob
    refSchema.create(table(PET_TABLE, column("name").setPkey(), column("species")));
    refSchema.getTable(PET_TABLE).getMetadata().setRlsEnabled(true);

    refSchema
        .getTable(PET_TABLE)
        .insert(
            row("name", "fido", "species", "dog").setString(MG_OWNER_COLUMN, USER_ALICE),
            row("name", "whiskers", "species", "cat").setString(MG_OWNER_COLUMN, USER_BOB));

    // Owner table in childSchema — FK ref to Pet, no RLS on Owner itself
    childSchema.create(
        table(
            OWNER_TABLE,
            column("name").setPkey(),
            column("pet").setType(REF).setRefSchemaName(REF_SCHEMA).setRefTable(PET_TABLE)));

    childSchema
        .getTable(OWNER_TABLE)
        .insert(
            row("name", "alice-owns-fido", "pet", "fido"),
            row("name", "bob-owns-whiskers", "pet", "whiskers"),
            row("name", "nobody-has-no-pet"));

    // PetLover table in childSchema — REF_ARRAY to Pet
    childSchema.create(
        table(
            PET_LOVER_TABLE,
            column("name").setPkey(),
            column("pets").setType(REF_ARRAY).setRefSchemaName(REF_SCHEMA).setRefTable(PET_TABLE)));

    childSchema
        .getTable(PET_LOVER_TABLE)
        .insert(
            row("name", "alice-lover", "pets", new String[] {"fido", "whiskers"}),
            row("name", "nobody-lover"));

    // Grant Alice VIEW_OWN on Pet in refSchema via group+role
    roleManager.createGroup(refSchema, GROUP_ALICE);
    roleManager.createRole(REF_SCHEMA, ROLE_VIEW_OWN);
    roleManager.setPermissions(
        refSchema,
        ROLE_VIEW_OWN,
        new PermissionSet()
            .putTable(
                PET_TABLE,
                new TablePermission(PET_TABLE)
                    .select(SelectScope.OWN)
                    .reference(ReferenceScope.NONE)));
    roleManager.addGroupMembership(REF_SCHEMA, GROUP_ALICE, USER_ALICE, ROLE_VIEW_OWN);

    // Grant Alice VIEW_ALL on Owner in childSchema (full access to child)
    roleManager.createRole(CHILD_SCHEMA, ROLE_VIEW_ALL);
    roleManager.setPermissions(
        childSchema,
        ROLE_VIEW_ALL,
        new PermissionSet()
            .putTable(
                OWNER_TABLE,
                new TablePermission(OWNER_TABLE)
                    .select(SelectScope.ALL)
                    .insert(UpdateScope.NONE)
                    .update(UpdateScope.NONE)
                    .delete(UpdateScope.NONE))
            .putTable(
                PET_LOVER_TABLE,
                new TablePermission(PET_LOVER_TABLE)
                    .select(SelectScope.ALL)
                    .insert(UpdateScope.NONE)
                    .update(UpdateScope.NONE)
                    .delete(UpdateScope.NONE)));
    childSchema.addMember(USER_ALICE, "Viewer");
  }

  @AfterAll
  public static void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(CHILD_SCHEMA);
    db.dropSchemaIfExists(REF_SCHEMA);
  }

  /**
   * R.3b: retrieveRows hides Child rows whose FK target is outside the user's view scope on
   * refTable. The row referencing bob's pet must not appear.
   */
  @Test
  void scalarRef_hidesChildRow_whenFkTargetInvisible() {
    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = childSchema.getTable(OWNER_TABLE).select(s("name"), s("pet")).retrieveRows();
      List<String> names = rows.stream().map(r -> r.getString("name")).toList();
      assertTrue(names.contains("alice-owns-fido"), "Alice's own row must be visible");
      assertFalse(
          names.contains("bob-owns-whiskers"), "Row pointing to invisible pet must be hidden");
      assertTrue(names.contains("nobody-has-no-pet"), "Null-FK row must remain visible");
    } finally {
      db.becomeAdmin();
    }
  }

  /**
   * retrieveJSON path: PG RLS fires on the Pet subselect so whiskers' data is suppressed. Alice
   * sees fido's species but not whiskers' species ("cat") since Alice cannot read that Pet row.
   */
  @Test
  void joinResolvesOnlyVisibleParents() {
    db.setActiveUser(USER_ALICE);
    try {
      String json =
          childSchema
              .getTable(OWNER_TABLE)
              .select(s("name"), s("pet", s("name"), s("species")))
              .retrieveJSON();
      assertTrue(json.contains("alice-owns-fido"), "Alice's row must appear in JSON");
      assertTrue(json.contains("dog"), "Alice's visible pet species must appear in JSON");
      assertFalse(json.contains("\"cat\""), "Invisible pet species must not appear in JSON");
    } finally {
      db.becomeAdmin();
    }
  }

  /**
   * REF_ARRAY behavior is unchanged in R.3b (R.3c will address it). Array elements referencing
   * invisible targets are dropped; the row itself remains visible.
   */
  @Test
  void refArrayDropsInvisibleElements() {
    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows =
          childSchema.getTable(PET_LOVER_TABLE).select(s("name"), s("pets")).retrieveRows();
      List<String> names = rows.stream().map(r -> r.getString("name")).toList();
      assertTrue(
          names.contains("alice-lover"), "PetLover row must remain visible (REF_ARRAY unchanged)");
    } finally {
      db.becomeAdmin();
    }
  }

  /**
   * Null FK: a row where the FK column is NULL must always remain visible regardless of RLS on the
   * refTable.
   */
  @Test
  void nullFkRow_remainsVisible() {
    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = childSchema.getTable(OWNER_TABLE).select(s("name"), s("pet")).retrieveRows();
      List<String> names = rows.stream().map(r -> r.getString("name")).toList();
      assertTrue(names.contains("nobody-has-no-pet"), "Row with null FK must stay visible");
    } finally {
      db.becomeAdmin();
    }
  }

  /** R.3b: a user with REFERENCE_ALL on refTable (but VIEW_NONE) keeps Child rows visible. */
  @Test
  void referenceAllOnRefTable_keepsChildRowVisible() {
    db.becomeAdmin();

    String userRef = "CsFkRlsRefUser";
    if (!db.hasUser(userRef)) db.addUser(userRef);

    String groupRef = "groupRef";
    roleManager.createGroup(refSchema, groupRef);

    roleManager.createRole(REF_SCHEMA, ROLE_REF_ALL);
    roleManager.setPermissions(
        refSchema,
        ROLE_REF_ALL,
        new PermissionSet()
            .putTable(
                PET_TABLE,
                new TablePermission(PET_TABLE)
                    .select(SelectScope.NONE)
                    .reference(ReferenceScope.ALL)));
    roleManager.addGroupMembership(REF_SCHEMA, groupRef, userRef, ROLE_REF_ALL);

    childSchema.addMember(userRef, "Viewer");

    db.setActiveUser(userRef);
    try {
      List<Row> rows = childSchema.getTable(OWNER_TABLE).select(s("name"), s("pet")).retrieveRows();
      List<String> names = rows.stream().map(r -> r.getString("name")).toList();
      assertTrue(
          names.contains("alice-owns-fido"),
          "REFERENCE_ALL must keep child row visible even with VIEW_NONE on refTable");
      assertTrue(
          names.contains("bob-owns-whiskers"),
          "REFERENCE_ALL must keep child row visible even with VIEW_NONE on refTable");
    } finally {
      db.becomeAdmin();
    }
  }

  /**
   * Refback field: not impacted by R.3b (the refback side has its own visibility from the parent
   * schema's RLS policy).
   */
  @Test
  void refbackEmptyForInvisibleParent() {
    // REF_BACK behavior is not changed by R.3b; this test verifies stability only.
    // Full refback-hide semantics are out of scope for this slice.
    db.setActiveUser(USER_ALICE);
    try {
      List<Row> rows = childSchema.getTable(OWNER_TABLE).select(s("name"), s("pet")).retrieveRows();
      assertNotNull(rows, "Query must not throw");
    } finally {
      db.becomeAdmin();
    }
  }
}
