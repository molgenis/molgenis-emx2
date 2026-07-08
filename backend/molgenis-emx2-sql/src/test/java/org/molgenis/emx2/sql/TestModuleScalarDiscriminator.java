package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TableType.MODULE;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestModuleScalarDiscriminator {

  private static final String SCHEMA = "TestModuleScalarDiscriminator";

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  private Schema freshSchema(String suffix) {
    String name = SCHEMA + suffix;
    db.dropSchemaIfExists(name);
    return db.createSchema(name);
  }

  // ── DDL: MODULE column stores as varchar (scalar), survives reload ─────────────

  @Test
  void moduleColumnPersistsAsVarcharAndSurvivesReload() {
    Schema s = freshSchema("Persist");
    String schemaName = s.getMetadata().getName();

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    Column modType = reloaded.getTable("Host").getMetadata().getColumn("modType");

    assertNotNull(modType, "modType column must survive reload");
    assertEquals(
        org.molgenis.emx2.ColumnType.MODULE, modType.getColumnType(), "type must remain MODULE");
    assertNotNull(modType.getValues(), "explicit values must survive reload");
    assertTrue(
        modType.getValues().contains("Mod"),
        "explicit values must contain the bare MODULE reference after reload");
  }

  // ── isDiscriminator returns true for MODULE ────────────────────────────────────

  @Test
  void moduleColumnIsDiscriminator() {
    Schema s = freshSchema("IsDisc");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    Column modType = s.getTable("Host").getMetadata().getColumn("modType");
    assertTrue(modType.isDiscriminator(), "MODULE column must be a discriminator");

    List<Column> discriminators = s.getTable("Host").getMetadata().getDiscriminatorColumns();
    assertEquals(1, discriminators.size(), "must have exactly one discriminator");
    assertEquals("modType", discriminators.get(0).getName());
  }

  // ── Derived-default values: MODULE with no explicit values → getEffectiveValues() returns
  // subclasses ──────────────────────────────────────────────────────────────────

  @Test
  void derivedDefaultValuesReturnModuleSubclassesWhenNoExplicitValuesSet() {
    Schema s = freshSchema("DerivedDefault");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("col1")));
    s.create(table("Mod2").setTableType(MODULE).setInheritNames("Host").add(column("col2")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE));

    Column modType = s.getTable("Host").getMetadata().getColumn("modType");

    assertNull(modType.getValues(), "getValues() must stay null (raw — not frozen)");

    List<String> effective = modType.getEffectiveValues();
    assertNotNull(effective, "getEffectiveValues() must return a non-null list");
    assertTrue(effective.contains("Mod1"), "effective values must include Mod1");
    assertTrue(effective.contains("Mod2"), "effective values must include Mod2");
  }

  // ── Derived-default: a later-added module appears in getEffectiveValues() ─────

  @Test
  void derivedDefaultValuesLiveNotFrozen() {
    Schema s = freshSchema("DerivedLive");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("col1")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE));

    Column modType = s.getTable("Host").getMetadata().getColumn("modType");
    List<String> beforeAdd = modType.getEffectiveValues();
    assertNotNull(beforeAdd);
    assertTrue(beforeAdd.contains("Mod1"), "Mod1 must be in effective values before adding Mod2");
    assertFalse(beforeAdd.contains("Mod2"), "Mod2 must NOT be in effective values yet");

    s.create(table("Mod2").setTableType(MODULE).setInheritNames("Host").add(column("col2")));

    List<String> afterAdd = modType.getEffectiveValues();
    assertTrue(
        afterAdd.contains("Mod2"), "Mod2 must appear in effective values after it is created");
  }

  // ── Explicit values OVERRIDES/restricts the derived default ───────────────────

  @Test
  void explicitValuesOverridesDerivedDefault() {
    Schema s = freshSchema("ExplicitOverride");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("col1")));
    s.create(table("Mod2").setTableType(MODULE).setInheritNames("Host").add(column("col2")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod1"));

    Column modType = s.getTable("Host").getMetadata().getColumn("modType");

    List<String> values = modType.getValues();
    assertNotNull(values, "getValues() must not be null when explicit values are set");
    assertEquals(List.of("Mod1"), values, "getValues() must return exactly the explicit values");

    List<String> effective = modType.getEffectiveValues();
    assertEquals(
        List.of("Mod1"), effective, "getEffectiveValues() must match explicit values when set");
    assertFalse(
        effective.contains("Mod2"), "Mod2 must be excluded by the explicit values override");
  }

  // ── Validation: values referencing a non-existent table → rejected ─────────────

  @Test
  void moduleValuesRejectNonExistentTable() {
    Schema s = freshSchema("NonExist");
    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(
                        column("modType")
                            .setType(org.molgenis.emx2.ColumnType.MODULE)
                            .setValues("DoesNotExist")));

    assertTrue(
        ex.getMessage().contains("DoesNotExist"),
        "Error must mention the missing table, got: " + ex.getMessage());
  }

  // ── Validation: values referencing a DATA table → rejected ────────────────────

  @Test
  void moduleValuesRejectDataTable() {
    Schema s = freshSchema("DataRej");

    s.create(table("DataTable").add(column("id").setPkey()));
    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(
                        column("modType")
                            .setType(org.molgenis.emx2.ColumnType.MODULE)
                            .setValues("DataTable")));

    assertTrue(
        ex.getMessage().contains("DataTable") || ex.getMessage().contains("MODULE"),
        "Error must mention the wrong-type table or MODULE requirement, got: " + ex.getMessage());
  }

  // ── Validation: dotted value → rejected ──────────────────────────────────────

  @Test
  void moduleValuesRejectDottedValue() {
    Schema s = freshSchema("Dotted");
    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(
                        column("modType")
                            .setType(org.molgenis.emx2.ColumnType.MODULE)
                            .setValues("schema.Table")));

    assertTrue(
        ex.getMessage().contains("bare table name") || ex.getMessage().contains("not supported"),
        "Error must describe bare-only requirement, got: " + ex.getMessage());
  }

  // ── Validation: MODULE must be declared on root table ─────────────────────────

  @Test
  void moduleColumnMustBeDeclaredOnRoot() {
    Schema s = freshSchema("RootOnly");

    s.create(table("Root").add(column("id").setPkey()));
    s.create(table("Sub").setInheritNames("Root").add(column("subCol").setType(STRING)));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Root").add(column("modCol")));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Sub")
                    .getMetadata()
                    .add(
                        column("modType")
                            .setType(org.molgenis.emx2.ColumnType.MODULE)
                            .setValues("Mod")),
            "MODULE column declared on a non-root DATA subtype must be rejected");

    assertTrue(
        ex.getMessage().contains("root") || ex.getMessage().contains("subtype"),
        "Error must mention root/subtype constraint, got: " + ex.getMessage());
  }

  // ── Validation: MODULE value must extend this root ────────────────────────────

  @Test
  void moduleValueMustExtendThisRoot() {
    Schema s = freshSchema("WrongRoot");

    s.create(table("RootA").add(column("id").setType(STRING).setPkey()));
    s.create(table("RootB").add(column("id").setType(STRING).setPkey()));

    s.create(
        table("ModForB")
            .setTableType(MODULE)
            .setInheritNames("RootB")
            .add(column("modCol").setType(STRING)));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("RootA")
                    .getMetadata()
                    .add(
                        column("modType")
                            .setType(org.molgenis.emx2.ColumnType.MODULE)
                            .setValues("ModForB")));

    assertTrue(
        ex.getMessage().contains("ModForB") || ex.getMessage().contains("root"),
        "Error must mention the non-extending module or root mismatch, got: " + ex.getMessage());
  }

  // ── INSERT: activates the chosen module row ────────────────────────────────────

  @Test
  void insertActivatesChosenModuleRow() {
    Schema s = freshSchema("InsertActivate");

    s.create(table("Host").add(column("id").setPkey()).add(column("hostCol")));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("mod1Col")));
    s.create(table("Mod2").setTableType(MODULE).setInheritNames("Host").add(column("mod2Col")));

    s.getTable("Host")
        .getMetadata()
        .add(
            column("modType")
                .setType(org.molgenis.emx2.ColumnType.MODULE)
                .setValues("Mod1", "Mod2"));

    s.getTable("Host").insert(row("id", "r1", "hostCol", "hv", "modType", "Mod1", "mod1Col", "v1"));

    List<Row> mod1Rows = s.getTable("Mod1").retrieveRows();
    assertEquals(1, mod1Rows.size(), "Mod1 must have a row");
    assertEquals("r1", mod1Rows.get(0).getString("id"));
    assertEquals("v1", mod1Rows.get(0).getString("mod1Col"));

    List<Row> mod2Rows = s.getTable("Mod2").retrieveRows();
    assertEquals(0, mod2Rows.size(), "Mod2 must have NO row (not activated)");

    List<Row> rootRows = s.getTable("Host").retrieveRows();
    assertEquals(1, rootRows.size());
    assertEquals(
        "Mod1", rootRows.get(0).getString("modType"), "scalar modType must store the chosen value");
  }

  // ── INSERT: enum membership rejects out-of-set value, accepts in-set ──────────

  @Test
  void insertRejectsOutOfSetValueAndAcceptsInSetValue() {
    Schema s = freshSchema("InsertMembership");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("mod1Col")));
    s.create(table("Mod2").setTableType(MODULE).setInheritNames("Host").add(column("mod2Col")));

    s.getTable("Host")
        .getMetadata()
        .add(
            column("modType")
                .setType(org.molgenis.emx2.ColumnType.MODULE)
                .setValues("Mod1", "Mod2"));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("Host").insert(row("id", "r1", "modType", "UnknownModule")),
        "Inserting a value not in allowed set must throw");

    assertDoesNotThrow(
        () -> s.getTable("Host").insert(row("id", "r2", "modType", "Mod1")),
        "Inserting an in-set value must succeed");
  }

  // ── INSERT: enum membership enforced via derived set when no explicit values ───

  @Test
  void insertWithDerivedSetEnforcesMembership() {
    Schema s = freshSchema("InsertDerived");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("mod1Col")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("Host").insert(row("id", "r1", "modType", "NotAModule")),
        "Value not in the derived effective set must be rejected");

    assertDoesNotThrow(
        () -> s.getTable("Host").insert(row("id", "r2", "modType", "Mod1")),
        "Value in the derived effective set must be accepted");
  }

  // ── UPDATE rejects out-of-set value ───────────────────────────────────────────

  @Test
  void updateRejectsOutOfSetValue() {
    Schema s = freshSchema("UpdateReject");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    s.getTable("Host").insert(row("id", "u1", "modType", "Mod"));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("Host").update(row("id", "u1", "modType", "NotAllowed")),
        "Updating to a value outside the allowed set must throw");
  }

  // ── C6: changing value deactivates old module (hard-delete) + activates new ───

  @Test
  void changeValueDeactivatesOldModuleAndActivatesNew() {
    Schema s = freshSchema("C6ChangeValue");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("col1")));
    s.create(table("Mod2").setTableType(MODULE).setInheritNames("Host").add(column("col2")));

    s.getTable("Host")
        .getMetadata()
        .add(
            column("modType")
                .setType(org.molgenis.emx2.ColumnType.MODULE)
                .setValues("Mod1", "Mod2"));

    s.getTable("Host").insert(row("id", "r1", "modType", "Mod1", "col1", "firstVal"));

    assertEquals(1, s.getTable("Mod1").retrieveRows().size(), "Mod1 must have a row after insert");
    assertEquals(0, s.getTable("Mod2").retrieveRows().size(), "Mod2 must have no row initially");

    s.getTable("Host").update(row("id", "r1", "modType", "Mod2", "col2", "secondVal"));

    assertEquals(
        0,
        s.getTable("Mod1").retrieveRows().size(),
        "Mod1 row must be hard-deleted after value change");
    assertEquals(
        1, s.getTable("Mod2").retrieveRows().size(), "Mod2 must have a row after activation");
    assertEquals(1, s.getTable("Host").retrieveRows().size(), "Root row must remain intact");

    List<Row> projected =
        s.getTable("Host").query().select(s("id"), s("col1"), s("col2")).retrieveRows();
    assertEquals(1, projected.size());
    assertNull(projected.get(0).getString("col1"), "col1 must be null after Mod1 deactivated");
    assertEquals(
        "secondVal", projected.get(0).getString("col2"), "col2 must project after Mod2 activated");
  }

  // ── C6: clearing the value deactivates the module ─────────────────────────────

  @Test
  void clearingValueDeactivatesModule() {
    Schema s = freshSchema("C6Clear");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    s.getTable("Host").insert(row("id", "r1", "modType", "Mod", "modCol", "val"));

    assertEquals(1, s.getTable("Mod").retrieveRows().size(), "Mod must have a row after insert");

    s.getTable("Host").update(row("id", "r1", "modType", (Object) null));

    assertEquals(
        0, s.getTable("Mod").retrieveRows().size(), "Mod row must be hard-deleted after clearing");
    assertEquals(1, s.getTable("Host").retrieveRows().size(), "Root row must remain intact");
  }

  // ── C4: query projects active module column, nulls when unset ─────────────────

  @Test
  void queryProjectsActiveModuleColumnNullsWhenUnset() {
    Schema s = freshSchema("C4Query");

    s.create(table("Host").add(column("id").setPkey()).add(column("hostCol")));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    s.getTable("Host")
        .insert(row("id", "activating", "hostCol", "hv", "modType", "Mod", "modCol", "modVal"));
    s.getTable("Host").insert(row("id", "nonActivating", "hostCol", "hv2"));

    List<Row> rows =
        s.getTable("Host").query().select(s("id"), s("modCol")).orderBy("id").retrieveRows();

    assertEquals(2, rows.size());

    Row activating =
        rows.stream().filter(r -> "activating".equals(r.getString("id"))).findFirst().orElseThrow();
    assertEquals("modVal", activating.getString("modCol"), "Activating row must project modCol");

    Row nonActivating =
        rows.stream()
            .filter(r -> "nonActivating".equals(r.getString("id")))
            .findFirst()
            .orElseThrow();
    assertNull(nonActivating.getString("modCol"), "Non-activating row must have null modCol");
  }

  // ── module-extends-module: activating a deep module writes the full chain ─────

  @Test
  void moduleExtendsModuleWritesFullChainInFkOrder() {
    Schema s = freshSchema("ModChain");

    s.create(table("Host").add(column("id").setPkey()).add(column("hostCol")));
    s.create(
        table("ModParent")
            .setTableType(MODULE)
            .setInheritNames("Host")
            .add(column("parentCol").setType(STRING)));
    s.create(
        table("ModChild")
            .setTableType(MODULE)
            .setInheritNames("ModParent")
            .add(column("childCol").setType(STRING)));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("ModChild"));

    assertDoesNotThrow(
        () ->
            s.getTable("Host")
                .insert(
                    row(
                        "id", "r1",
                        "hostCol", "hv",
                        "modType", "ModChild",
                        "parentCol", "pv",
                        "childCol", "cv")),
        "Insert with module-extends-module chain must succeed without FK violation");

    assertEquals(1, s.getTable("ModParent").retrieveRows().size(), "ModParent row must be written");
    assertEquals(1, s.getTable("ModChild").retrieveRows().size(), "ModChild row must be written");
    assertEquals("pv", s.getTable("ModParent").retrieveRows().get(0).getString("parentCol"));
    assertEquals("cv", s.getTable("ModChild").retrieveRows().get(0).getString("childCol"));
  }

  // ── required column only enforced when module is active ───────────────────────

  @Test
  void requiredModuleColumnEnforcedOnlyWhenModuleActive() {
    Schema s = freshSchema("RequiredGate");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Host")
            .add(column("reqCol").setRequired(true)));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    MolgenisException arm1 =
        assertThrows(
            MolgenisException.class,
            () -> s.getTable("Host").insert(row("id", "p1", "modType", "Mod")),
            "Active module with missing required column must be rejected");
    assertTrue(
        arm1.getMessage().contains("reqCol") || arm1.getMessage().contains("required"),
        "Error must mention the required column, got: " + arm1.getMessage());

    assertDoesNotThrow(
        () -> s.getTable("Host").insert(row("id", "p2", "modType", "Mod", "reqCol", "val")),
        "Active module with required column supplied must succeed");

    assertDoesNotThrow(
        () -> s.getTable("Host").insert(row("id", "p3")),
        "Inactive module must NOT enforce its required column");
  }

  // ── coexistence: MODULE scalar + MODULE_ARRAY on same root ────────────────────

  @Test
  void moduleScalarCoexistsWithModuleArray() {
    Schema s = freshSchema("Coexist");
    String schemaName = s.getMetadata().getName();

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("ModA").setTableType(MODULE).setInheritNames("Host").add(column("colA")));
    s.create(table("ModB").setTableType(MODULE).setInheritNames("Host").add(column("colB")));
    s.create(table("ModC").setTableType(MODULE).setInheritNames("Host").add(column("colC")));

    s.getTable("Host")
        .getMetadata()
        .add(column("scalar").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("ModA"))
        .add(column("arrayDisc").setType(MODULE_ARRAY).setValues("ModB", "ModC"));

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    List<Column> discriminators = reloaded.getTable("Host").getMetadata().getDiscriminatorColumns();
    assertEquals(2, discriminators.size(), "Both MODULE and MODULE_ARRAY must be discriminators");

    boolean hasScalar =
        discriminators.stream()
            .anyMatch(c -> c.getColumnType() == org.molgenis.emx2.ColumnType.MODULE);
    boolean hasArray = discriminators.stream().anyMatch(c -> c.getColumnType() == MODULE_ARRAY);
    assertTrue(hasScalar, "MODULE scalar discriminator must be present");
    assertTrue(hasArray, "MODULE_ARRAY discriminator must be present");
  }

  // ── getValues() stays RAW (empty) while getEffectiveValues() is derived ───────

  @Test
  void getValuesStaysRawAndGetEffectiveValuesDerives() {
    Schema s = freshSchema("RawVsDerived");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE));

    Column modType = s.getTable("Host").getMetadata().getColumn("modType");

    assertNull(modType.getValues(), "getValues() must return null (raw — not frozen)");

    List<String> effective = modType.getEffectiveValues();
    assertNotNull(effective, "getEffectiveValues() must return non-null");
    assertFalse(effective.isEmpty(), "getEffectiveValues() must return at least one module name");
    assertTrue(effective.contains("Mod"), "getEffectiveValues() must contain Mod");
  }

  // ── getEffectiveValues() == getValues() for MODULE_ARRAY (zero regression) ────

  @Test
  void effectiveValuesEqualsRawValuesForModuleArray() {
    Schema s = freshSchema("ArrayRegression");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host").getMetadata().add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    Column panels = s.getTable("Host").getMetadata().getColumn("panels");

    assertNotNull(panels.getValues(), "MODULE_ARRAY getValues() must be non-null");
    assertEquals(
        panels.getValues(),
        panels.getEffectiveValues(),
        "For MODULE_ARRAY, getEffectiveValues() must equal getValues() (zero regression)");
  }

  // ── E1: default-select flows module column implicitly ─────────────────────────

  @Test
  void defaultSelectIncludesActiveModuleColumn() {
    Schema s = freshSchema("E1ActiveMod");

    s.create(table("Host").add(column("id").setType(STRING).setPkey()).add(column("hostCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Host")
            .add(column("modCol").setType(STRING)));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    s.getTable("Host")
        .insert(row("id", "r1", "hostCol", "hv", "modType", "Mod", "modCol", "modValue"));

    List<Row> rows = s.getTable("Host").retrieveRows();
    assertEquals(1, rows.size());
    assertEquals(
        "modValue",
        rows.get(0).getString("modCol"),
        "Active module col must appear in default-select result");
  }

  @Test
  void defaultSelectNullsInactiveModuleColumn() {
    Schema s = freshSchema("E1InactiveMod");

    s.create(table("Host").add(column("id").setType(STRING).setPkey()).add(column("hostCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Host")
            .add(column("modCol").setType(STRING)));

    s.getTable("Host")
        .getMetadata()
        .add(column("modType").setType(org.molgenis.emx2.ColumnType.MODULE).setValues("Mod"));

    s.getTable("Host").insert(row("id", "r1", "hostCol", "hv"));

    List<Row> rows = s.getTable("Host").retrieveRows();
    assertEquals(1, rows.size());
    assertNull(
        rows.get(0).getString("modCol"), "Inactive module col must be null in default-select");
  }
}
