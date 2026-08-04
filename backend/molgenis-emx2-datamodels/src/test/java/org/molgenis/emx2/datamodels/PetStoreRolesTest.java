package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Constants.MG_ROLES;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.TestDatabaseFactory;

/** Verifies the molgenis_roles.csv of the PetStore profile is applied on import. */
class PetStoreRolesTest {

  private static final String SCHEMA = "PetStoreRolesTest";
  private static final String DRAGON_KEEPER = "DragonKeeper";
  private static final String DRAGON_KEEPER_USER = "dragonkeeper";

  private static Database database;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
    database.becomeAdmin();
    database.dropSchemaIfExists(SCHEMA);
    DataModels.Profile.PET_STORE.getImportTask(database, SCHEMA, "petstore roles", true).run();
  }

  @Test
  void dragonKeeperRoleIsCreatedWithRowLevelPermissionOnPet() {
    database.becomeAdmin();
    Role role = database.getSchema(SCHEMA).getRoleInfo(DRAGON_KEEPER);
    assertFalse(role.isSystemRole());
    TablePermission permission =
        role.permissions().stream()
            .filter(p -> "Pet".equals(p.table()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("no permission on Pet: " + role.permissions()));
    assertEquals(Boolean.TRUE, permission.select());
    assertEquals(Boolean.TRUE, permission.insert());
    assertEquals(Boolean.TRUE, permission.update());
    assertEquals(Boolean.TRUE, permission.delete());
    assertEquals(Boolean.TRUE, permission.isRowLevel());
  }

  @Test
  void dragonKeeperUserIsMemberOfDragonKeeperRole() {
    database.becomeAdmin();
    assertTrue(
        database.getSchema(SCHEMA).getMembers().stream()
            .anyMatch(
                m -> DRAGON_KEEPER_USER.equals(m.getUser()) && DRAGON_KEEPER.equals(m.getRole())),
        "dragonkeeper should be member of DragonKeeper");
  }

  @Test
  void smaugDemoRowIsTaggedWithDragonKeeperRole() {
    database.becomeAdmin();
    List<Row> rows =
        database
            .getSchema(SCHEMA)
            .getTable("Pet")
            .query()
            .select(SelectColumn.s("name"), SelectColumn.s(MG_ROLES))
            .where(FilterBean.f("name", Operator.EQUALS, "smaug"))
            .retrieveRows();
    assertEquals(1, rows.size(), "smaug should be in the demo data");
    assertArrayEquals(new String[] {DRAGON_KEEPER}, rows.get(0).getStringArray(MG_ROLES));
  }

  /**
   * The PetStore grants 'anonymous' the Viewer role and every user inherits the anonymous user
   * role, so every user hits the VIEWER_BYPASS policy and sees all rows: the row-level grant on
   * DragonKeeper restricts nobody here. This test pins that down; if the PetStore ever stops
   * granting anonymous Viewer, it should start failing and the demo becomes meaningful.
   */
  @Test
  void everyoneSeesSmaugBecausePetStoreGrantsAnonymousViewer() {
    for (String user : List.of(DRAGON_KEEPER_USER, "shopviewer", "anonymous")) {
      database.setActiveUser(user);
      List<String> names =
          database.getSchema(SCHEMA).getTable("Pet").retrieveRows().stream()
              .map(r -> r.getString("name"))
              .toList();
      assertTrue(names.contains("smaug"), user + " should see smaug via the Viewer bypass");
    }
    database.becomeAdmin();
  }

  /**
   * The row-level grant on DragonKeeper enables row level security on Pet, and the Aggregator role
   * has no bypass policy. Once the schema-wide Viewer that PetStore hands to 'anonymous' is taken
   * away, an aggregator therefore sees zero rows and the group by comes back empty rather than
   * aggregating the rows it is allowed to count. Uses its own schema so removing the anonymous
   * member does not leak into the other tests.
   */
  @Test
  void aggregatorSeesNoRowsOnRowLevelSecuredTable() {
    String aggregatorSchema = SCHEMA + "Agg";
    String aggregator = "petstore_roles_aggregator";
    database.becomeAdmin();
    database.dropSchemaIfExists(aggregatorSchema);
    DataModels.Profile.PET_STORE
        .getImportTask(database, aggregatorSchema, "petstore aggregator", true)
        .run();
    Schema schema = database.getSchema(aggregatorSchema);
    schema.removeMember(Constants.ANONYMOUS);
    if (!database.hasUser(aggregator)) database.addUser(aggregator);
    schema.addMember(aggregator, Privileges.AGGREGATOR.toString());

    database.setActiveUser(aggregator);
    String json =
        database
            .getSchema(aggregatorSchema)
            .query(
                "Pet_groupBy",
                SelectColumn.s("count"),
                SelectColumn.s("tags", SelectColumn.s("name")))
            .retrieveJSON();
    assertEquals("{\"Pet_groupBy\": null}", json);
    database.becomeAdmin();
  }
}
