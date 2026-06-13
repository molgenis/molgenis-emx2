package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.TableType.MODULE;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

/**
 * Integration tests for the MODULE_ARRAY discriminator column type: DDL storage, column-definition
 * and row-write validation, ENUM membership enforcement, and coexistence with mg_tableclass
 * inheritance.
 */
class TestModuleArrayDiscriminator {

  private static final String SCHEMA = "TestModuleArrayDiscriminator";

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  // ── helpers ──────────────────────────────────────────────────────────────────

  private Schema freshSchema(String suffix) {
    String name = SCHEMA + suffix;
    db.dropSchemaIfExists(name);
    return db.createSchema(name);
  }

  // ── test 1: MODULE_ARRAY column with valid MODULE values persists as varchar[] and
  // survives clearCache + reload ──────────────────────────────────────────────

  @Test
  void moduleArrayColumnPersistsAndSurvivesReload() {
    Schema s = freshSchema("Persist");
    String schemaName = s.getMetadata().getName();

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Host").add(column("modCol")));

    s.getTable("Host").getMetadata().add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    Column panels = reloaded.getTable("Host").getMetadata().getColumn("panels");

    assertNotNull(panels, "panels column must survive reload");
    assertEquals(MODULE_ARRAY, panels.getColumnType(), "column type must remain MODULE_ARRAY");
    assertNotNull(panels.getValues(), "values must survive reload");
    assertTrue(
        panels.getValues().contains("Mod"),
        "values must contain the bare MODULE reference after reload");

    TableMetadata modMeta = reloaded.getTable("Mod").getMetadata();
    assertEquals(MODULE, modMeta.getTableType(), "tableType MODULE must survive reload");
  }

  // ── test 2: values referencing a non-existent table → rejected ──────────────

  @Test
  void moduleArrayValuesRejectNonExistentTable() {
    Schema s = freshSchema("NonExist");
    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("DoesNotExist")));

    assertTrue(
        ex.getMessage().contains("DoesNotExist"),
        "Error must mention the missing table, got: " + ex.getMessage());
  }

  // ── test 3: values referencing a DATA table → rejected ──────────────────────

  @Test
  void moduleArrayValuesRejectDataTable() {
    Schema s = freshSchema("DataRej");

    s.create(table("DataTable").add(column("id").setPkey()));
    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("DataTable")));

    assertTrue(
        ex.getMessage().contains("DataTable") || ex.getMessage().contains("MODULE"),
        "Error must mention the wrong-type table or MODULE requirement, got: " + ex.getMessage());
  }

  // ── test 3b: values referencing an ONTOLOGIES table → rejected ───────────────

  @Test
  void moduleArrayValuesRejectOntologyTable() {
    Schema s = freshSchema("OntRej");

    s.create(
        table("OntTable")
            .setTableType(TableType.ONTOLOGIES)
            .add(column("name").setPkey())
            .add(column("label")));
    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("OntTable")));

    assertTrue(
        ex.getMessage().contains("OntTable") || ex.getMessage().contains("MODULE"),
        "Error must mention the wrong-type table or MODULE requirement, got: " + ex.getMessage());
  }

  // ── test 3c: any dotted value → rejected; bare name → legal ──────────────────

  @Test
  void moduleArrayValuesRejectAnyDottedValueAcceptBareName() {
    Schema s = freshSchema("Malformed");

    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException trailingDot =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("schema.")),
            "Trailing-dot value must throw MolgenisException");
    assertTrue(
        trailingDot.getMessage().contains("bare table name")
            || trailingDot.getMessage().contains("not supported"),
        "Error must describe bare-only requirement, got: " + trailingDot.getMessage());

    MolgenisException qualifiedValue =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("schema.Table")),
            "Schema-qualified value must be rejected");
    assertTrue(
        qualifiedValue.getMessage().contains("bare table name")
            || qualifiedValue.getMessage().contains("not supported"),
        "Error must describe bare-only requirement, got: " + qualifiedValue.getMessage());

    MolgenisException leadingDot =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues(".Mod")),
            "Leading-dot value must be rejected");
    assertTrue(
        leadingDot.getMessage().contains("bare table name")
            || leadingDot.getMessage().contains("not supported"),
        "Error must describe bare-only requirement for leading-dot, got: "
            + leadingDot.getMessage());
  }

  // ── test 4: INSERT element not in allowed set → rejected; allowed → accepted ──

  @Test
  void insertRejectsOutOfSetValueAndAcceptsInSetValue() {
    Schema s = freshSchema("Insert");

    s.create(table("Host").add(column("id").setPkey()));
    s.create(table("Mod1").setTableType(MODULE).setInheritNames("Host").add(column("mod1Col")));
    s.create(table("Mod2").setTableType(MODULE).setInheritNames("Host").add(column("mod2Col")));

    s.getTable("Host")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod1", "Mod2"));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("Host").insert(row("id", "r1", "panels", "UnknownModule")),
        "Inserting an element not in values must throw");

    assertDoesNotThrow(
        () -> s.getTable("Host").insert(row("id", "r2", "panels", "Mod1")),
        "Inserting an element in values must succeed");
  }

  // ── test 5: UPDATE to out-of-set value → rejected ───────────────────────────

  @Test
  void updateRejectsOutOfSetValue() {
    Schema s = freshSchema("Update");

    s.create(table("HostU").add(column("id").setPkey()));
    s.create(table("ModU").setTableType(MODULE).setInheritNames("HostU").add(column("modUCol")));

    s.getTable("HostU").getMetadata().add(column("panels").setType(MODULE_ARRAY).setValues("ModU"));

    s.getTable("HostU").insert(row("id", "u1", "panels", "ModU"));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("HostU").update(row("id", "u1", "panels", "NotAllowed")),
        "Updating to a value not in the allowed set must throw");
  }

  // ── test 6a: ENUM scalar with values: out-of-set → rejected; in-set → accepted ─

  @Test
  void enumScalarWithValuesEnforcesAllowedSet() {
    Schema s = freshSchema("EnumScalar");

    s.create(
        table("Triage")
            .add(column("id").setPkey())
            .add(column("priority").setType(ENUM).setValues("low", "medium", "high")));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("Triage").insert(row("id", "t1", "priority", "critical")),
        "Inserting a value outside ENUM values must throw");

    assertDoesNotThrow(
        () -> s.getTable("Triage").insert(row("id", "t2", "priority", "high")),
        "Inserting an in-set ENUM value must succeed");
  }

  // ── test 6b: ENUM with NO values → free string accepted (back-compat) ────────

  @Test
  void enumWithNoValuesFreeStringAccepted() {
    Schema s = freshSchema("EnumFree");

    s.create(table("FreeEnum").add(column("id").setPkey()).add(column("status").setType(ENUM)));

    assertDoesNotThrow(
        () -> s.getTable("FreeEnum").insert(row("id", "f1", "status", "anything")),
        "ENUM with no declared values must accept any string (back-compat)");
  }

  // ── test 7: coexistence of extends (mg_tableclass) + MODULE_ARRAY columns ────

  @Test
  void mgTableclassAndModuleArrayCoexist() {
    Schema s = freshSchema("Coexist");
    String schemaName = s.getMetadata().getName();

    s.create(table("Animal").add(column("id").setType(STRING).setPkey()).add(column("name")));
    s.create(table("Dog").setInheritNames("Animal").add(column("breed")));

    s.create(
        table("PanelA").setTableType(MODULE).setInheritNames("Animal").add(column("panelACol")));
    s.create(
        table("PanelB").setTableType(MODULE).setInheritNames("Animal").add(column("panelBCol")));
    s.create(
        table("PanelC").setTableType(MODULE).setInheritNames("Animal").add(column("panelCCol")));

    s.getTable("Animal")
        .getMetadata()
        .add(column("axis1").setType(MODULE_ARRAY).setValues("PanelA", "PanelB"))
        .add(column("axis2").setType(MODULE_ARRAY).setValues("PanelC"));

    s.create(table("DogWithPanels").setInheritNames("Dog").add(column("extra")));

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    TableMetadata dogWithPanelsMeta = reloaded.getTable("DogWithPanels").getMetadata();

    Column mgTableclass = reloaded.getTable("Animal").getMetadata().getLocalColumn(MG_TABLECLASS);
    assertNotNull(mgTableclass, "mg_tableclass must exist on root Animal");
    assertEquals(
        Boolean.TRUE,
        mgTableclass.isReadonly(),
        "mg_tableclass must be readonly (immutable via the write API) when extends is used");

    List<Column> discriminators = dogWithPanelsMeta.getDiscriminatorColumns();
    assertEquals(2, discriminators.size(), "DogWithPanels must have 2 MODULE_ARRAY discriminators");

    Column axis1 = dogWithPanelsMeta.getColumn("axis1");
    Column axis2 = dogWithPanelsMeta.getColumn("axis2");
    assertNotNull(axis1);
    assertNotNull(axis2);
    assertEquals(MODULE_ARRAY, axis1.getColumnType());
    assertEquals(MODULE_ARRAY, axis2.getColumnType());
  }

  // ── C2.B: MODULE-extends-root → real table, PK+FK+KEY1, NO mg_tableclass ─────

  @Test
  void moduleExtendsRootIsRealTableWithPkFkKey1NoMgTableclass() {
    Schema s = freshSchema("C2B");
    String schemaName = s.getMetadata().getName();

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);

    TableMetadata modMeta = reloaded.getTable("Mod").getMetadata();
    assertNotNull(modMeta, "Module table Mod must exist (real physical table)");

    List<String> modPrimaryKeys = modMeta.getPrimaryKeys();
    assertEquals(1, modPrimaryKeys.size(), "Mod must have exactly one PK column (root's id)");
    assertEquals("id", modPrimaryKeys.get(0), "Mod's PK must be Root's id");

    List<String> modParents = modMeta.getInheritNames();
    assertTrue(modParents.contains("Root"), "Mod must list Root as parent after reload");

    Column mgTableclassOnRoot =
        reloaded.getTable("Root").getMetadata().getLocalColumn(MG_TABLECLASS);
    assertNull(
        mgTableclassOnRoot,
        "Root must NOT have mg_tableclass when only a MODULE (not a DATA is-a subtype) extends it");
  }

  // ── C2.C: MODULE subtypes excluded from is-a getSubclassTables ───────────────

  @Test
  void moduleSubtypesExcludedFromIsAEnumeration() {
    Schema s = freshSchema("C2C");
    String schemaName = s.getMetadata().getName();

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(table("DataSub").setInheritNames("Root").add(column("dataCol").setType(STRING)));
    s.create(
        table("ModSub")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);

    List<TableMetadata> subclasses = reloaded.getTable("Root").getMetadata().getSubclassTables();
    List<String> subclassNames = subclasses.stream().map(TableMetadata::getTableName).toList();

    assertTrue(
        subclassNames.contains("DataSub"),
        "DataSub (DATA subtype) must appear in getSubclassTables()");
    assertFalse(
        subclassNames.contains("ModSub"),
        "ModSub (MODULE subtype) must NOT appear in getSubclassTables()");

    assertNotNull(reloaded.getTable("ModSub"), "ModSub must still exist in catalog (real table)");
  }

  // ── C2.D: MODULE_ARRAY value must extend this root; one-axis-per-module ──────

  @Test
  void moduleArrayValueMustExtendThisRoot() {
    Schema s = freshSchema("C2D_root");

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
                    .add(column("panels").setType(MODULE_ARRAY).setValues("ModForB")),
            "MODULE_ARRAY value must reference a module that extends THIS root, not a different root");

    assertTrue(
        ex.getMessage().contains("ModForB") || ex.getMessage().contains("root"),
        "Error must mention the non-extending module or root mismatch, got: " + ex.getMessage());
  }

  @Test
  void moduleArrayValueOneAxisPerModule() {
    Schema s = freshSchema("C2D_axis");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));

    s.getTable("Root").getMetadata().add(column("axis1").setType(MODULE_ARRAY).setValues("Mod"));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Root")
                    .getMetadata()
                    .add(column("axis2").setType(MODULE_ARRAY).setValues("Mod")),
            "A module may only appear in one MODULE_ARRAY axis per table (O-5)");

    assertTrue(
        ex.getMessage().contains("Mod") || ex.getMessage().contains("axis"),
        "Error must mention the duplicate module or axis conflict, got: " + ex.getMessage());
  }

  // ── C3: write routing into module subtype tables ─────────────────────────────

  @Test
  void insertActivatingTwoModulesWritesRowInEachModuleTable() {
    Schema s = freshSchema("C3TwoMods");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("mod2Col").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "r1",
                "rootCol", "rootValue",
                "panels", new String[] {"Mod", "Mod2"},
                "modCol", "modValue",
                "mod2Col", "mod2Value"));

    List<Row> modRows = s.getTable("Mod").retrieveRows();
    assertEquals(1, modRows.size(), "Mod must have exactly one row for the activated root PK");
    assertEquals("r1", modRows.get(0).getString("id"), "Mod row must have the shared root PK");
    assertEquals("modValue", modRows.get(0).getString("modCol"), "Mod row must have modCol");

    List<Row> mod2Rows = s.getTable("Mod2").retrieveRows();
    assertEquals(1, mod2Rows.size(), "Mod2 must have exactly one row for the activated root PK");
    assertEquals("r1", mod2Rows.get(0).getString("id"), "Mod2 row must have the shared root PK");
    assertEquals("mod2Value", mod2Rows.get(0).getString("mod2Col"), "Mod2 row must have mod2Col");

    List<Row> rootRows = s.getTable("Root").retrieveRows();
    assertEquals(1, rootRows.size(), "Root must have exactly one row");
    String[] panels = rootRows.get(0).getStringArray("panels");
    assertNotNull(panels, "panels must not be null on root row");
    assertEquals(2, panels.length, "panels must contain both module references");
  }

  @Test
  void insertActivatingOneModuleWritesOnlyThatModuleRow() {
    Schema s = freshSchema("C3OneMod");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("mod2Col").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "r1",
                "rootCol", "rootValue",
                "panels", "Mod",
                "modCol", "modValue"));

    List<Row> modRows = s.getTable("Mod").retrieveRows();
    assertEquals(1, modRows.size(), "Mod must have a row since it was activated");
    assertEquals("r1", modRows.get(0).getString("id"), "Mod row must have the shared root PK");

    List<Row> mod2Rows = s.getTable("Mod2").retrieveRows();
    assertEquals(0, mod2Rows.size(), "Mod2 must have NO row since it was not activated");
  }

  @Test
  void updateUpsertsNewlyActivatedModuleRow() {
    Schema s = freshSchema("C3Upsert");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("mod2Col").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "r1",
                "rootCol", "rootValue",
                "panels", "Mod",
                "modCol", "modValue"));

    List<Row> mod2Before = s.getTable("Mod2").retrieveRows();
    assertEquals(0, mod2Before.size(), "Mod2 must have no row before activation");

    s.getTable("Root")
        .update(
            row(
                "id", "r1",
                "panels", new String[] {"Mod", "Mod2"},
                "mod2Col", "mod2Value"));

    // C6: removed-module hard-delete is deferred; only upsert-on-activate is tested here
    List<Row> mod2After = s.getTable("Mod2").retrieveRows();
    assertEquals(1, mod2After.size(), "Mod2 must have a row after being activated via update");
    assertEquals("r1", mod2After.get(0).getString("id"), "Mod2 row must share the root PK");
    assertEquals(
        "mod2Value", mod2After.get(0).getString("mod2Col"), "Mod2 row must have the updated value");
  }

  // ── C3: module-extends-module — FK ordering: intermediate module written before leaf ──────────

  @Test
  void moduleExtendsModuleWritesFullModuleChainInFkOrder() {
    Schema s = freshSchema("C3ModChain");

    // ModChild extends ModParent which extends Root:
    //   Root(id PK, rootCol) ← ModParent(MODULE, parentCol) ← ModChild(MODULE, childCol)
    // Root declares panels = [ModChild]. Inserting a Root row that activates ModChild must
    // write ModParent BEFORE ModChild to satisfy the FK from ModChild.id → ModParent.id.
    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("ModParent")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("parentCol").setType(STRING)));
    s.create(
        table("ModChild")
            .setTableType(MODULE)
            .setInheritNames("ModParent")
            .add(column("childCol").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("ModChild"));

    // INSERT must succeed — FK integrity is maintained by writing ModParent before ModChild
    assertDoesNotThrow(
        () ->
            s.getTable("Root")
                .insert(
                    row(
                        "id", "r1",
                        "rootCol", "rootValue",
                        "panels", "ModChild",
                        "parentCol", "parentValue",
                        "childCol", "childValue")),
        "Insert activating a two-level MODULE chain must succeed without FK violation");

    List<Row> parentRows = s.getTable("ModParent").retrieveRows();
    assertEquals(1, parentRows.size(), "ModParent must have exactly one row (intermediate write)");
    assertEquals("r1", parentRows.get(0).getString("id"), "ModParent row must share the root PK");
    assertEquals(
        "parentValue",
        parentRows.get(0).getString("parentCol"),
        "ModParent row must carry parentCol");

    List<Row> childRows = s.getTable("ModChild").retrieveRows();
    assertEquals(1, childRows.size(), "ModChild must have exactly one row");
    assertEquals("r1", childRows.get(0).getString("id"), "ModChild row must share the root PK");
    assertEquals(
        "childValue", childRows.get(0).getString("childCol"), "ModChild row must carry childCol");

    List<Row> rootRows = s.getTable("Root").retrieveRows();
    assertEquals(1, rootRows.size(), "Root must have exactly one row");
    String[] panels = rootRows.get(0).getStringArray("panels");
    assertNotNull(panels, "panels must not be null on root row");
    assertEquals(1, panels.length, "panels must contain ModChild reference");
    assertEquals("ModChild", panels[0], "panels must store the bare ModChild reference");
  }

  // ── C2.E: reload round-trip — module subtypes + MODULE_ARRAY survive reload ──

  @Test
  void moduleSubtypesAndModuleArraySurviveReload() {
    Schema s = freshSchema("C2E");
    String schemaName = s.getMetadata().getName();

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("ModOne")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("oneCol").setType(STRING)));
    s.create(
        table("ModTwo")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("twoCol").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("ModOne", "ModTwo"));

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);

    TableMetadata rootMeta = reloaded.getTable("Root").getMetadata();
    TableMetadata modOneMeta = reloaded.getTable("ModOne").getMetadata();
    TableMetadata modTwoMeta = reloaded.getTable("ModTwo").getMetadata();

    assertNotNull(modOneMeta, "ModOne must survive reload");
    assertNotNull(modTwoMeta, "ModTwo must survive reload");
    assertEquals(MODULE, modOneMeta.getTableType(), "ModOne tableType must survive reload");
    assertEquals(MODULE, modTwoMeta.getTableType(), "ModTwo tableType must survive reload");
    assertTrue(
        modOneMeta.getInheritNames().contains("Root"),
        "ModOne must still extend Root after reload");
    assertTrue(
        modTwoMeta.getInheritNames().contains("Root"),
        "ModTwo must still extend Root after reload");

    Column panels = rootMeta.getColumn("panels");
    assertNotNull(panels, "MODULE_ARRAY column 'panels' must survive reload");
    assertEquals(
        MODULE_ARRAY, panels.getColumnType(), "panels type must be MODULE_ARRAY after reload");
    assertNotNull(panels.getValues(), "panels.values must survive reload");
    assertTrue(
        panels.getValues().contains("ModOne"), "panels.values must contain bare ModOne reference");
    assertTrue(
        panels.getValues().contains("ModTwo"), "panels.values must contain bare ModTwo reference");

    List<String> isASubclasses =
        rootMeta.getSubclassTables().stream().map(TableMetadata::getTableName).toList();
    assertFalse(
        isASubclasses.contains("ModOne"),
        "ModOne must NOT be in is-a getSubclassTables() after reload");
    assertFalse(
        isASubclasses.contains("ModTwo"),
        "ModTwo must NOT be in is-a getSubclassTables() after reload");
  }

  // ── C4: query projection of active module columns ────────────────────────────

  @Test
  void queryRootProjectsActiveModuleColumns() {
    Schema s = freshSchema("C4TwoMods");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("mod2Col").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "r1",
                "rootCol", "rootValue",
                "panels", new String[] {"Mod", "Mod2"},
                "modCol", "modValue",
                "mod2Col", "mod2Value"));

    List<Row> rows =
        s.getTable("Root").query().select(s("id"), s("modCol"), s("mod2Col")).retrieveRows();

    assertEquals(1, rows.size(), "Root query must return exactly one row");
    Row row = rows.get(0);
    assertEquals("r1", row.getString("id"));
    assertEquals("modValue", row.getString("modCol"), "Active module col modCol must project");
    assertEquals("mod2Value", row.getString("mod2Col"), "Active module col mod2Col must project");
  }

  @Test
  void queryRootNullsInactiveModuleColumn() {
    Schema s = freshSchema("C4OneMod");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("mod2Col").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "r1",
                "rootCol", "rootValue",
                "panels", "Mod",
                "modCol", "modValue"));

    List<Row> rows =
        s.getTable("Root").query().select(s("id"), s("modCol"), s("mod2Col")).retrieveRows();

    assertEquals(1, rows.size());
    Row row = rows.get(0);
    assertEquals("modValue", row.getString("modCol"), "Active module col modCol must project");
    assertNull(row.getString("mod2Col"), "Inactive module col mod2Col must be NULL (no row)");
  }

  @Test
  void queryRootProjectsModuleExtendsModuleChain() {
    Schema s = freshSchema("C4ModChain");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("ModParent")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("parentCol").setType(STRING)));
    s.create(
        table("ModChild")
            .setTableType(MODULE)
            .setInheritNames("ModParent")
            .add(column("childCol").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("ModChild"));

    s.getTable("Root")
        .insert(
            row(
                "id", "r1",
                "rootCol", "rootValue",
                "panels", "ModChild",
                "parentCol", "parentValue",
                "childCol", "childValue"));

    List<Row> rows =
        s.getTable("Root").query().select(s("id"), s("parentCol"), s("childCol")).retrieveRows();

    assertEquals(1, rows.size());
    Row row = rows.get(0);
    assertEquals("parentValue", row.getString("parentCol"), "Ancestor module col must project");
    assertEquals("childValue", row.getString("childCol"), "Leaf module col must project");
  }

  @Test
  void queryMixedBatchProjectsPerRowModuleColumns() {
    Schema s = freshSchema("C4MixedBatch");

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod1")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("mod1Col").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("mod2Col").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod1", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "rowA",
                "rootCol", "aVal",
                "panels", "Mod1",
                "mod1Col", "aModValue"));

    s.getTable("Root")
        .insert(
            row(
                "id", "rowB",
                "rootCol", "bVal",
                "panels", "Mod2",
                "mod2Col", "bModValue"));

    List<Row> rows =
        s.getTable("Root")
            .query()
            .select(s("id"), s("mod1Col"), s("mod2Col"))
            .orderBy("id")
            .retrieveRows();

    assertEquals(2, rows.size(), "Root query must return both rows");

    Row rowA = rows.get(0);
    assertEquals("rowA", rowA.getString("id"));
    assertEquals("aModValue", rowA.getString("mod1Col"), "rowA: mod1Col must project");
    assertNull(rowA.getString("mod2Col"), "rowA: mod2Col must be NULL (Mod2 not activated)");

    Row rowB = rows.get(1);
    assertEquals("rowB", rowB.getString("id"));
    assertNull(rowB.getString("mod1Col"), "rowB: mod1Col must be NULL (Mod1 not activated)");
    assertEquals("bModValue", rowB.getString("mod2Col"), "rowB: mod2Col must project");
  }

  // ── bare-value defaulting tests ──────────────────────────────────────────────

  @Test
  void moduleArrayAcceptsBareValueDefaultingToCurrentSchema() {
    Schema s = freshSchema("BareDecl");
    String schemaName = s.getMetadata().getName();

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));

    assertDoesNotThrow(
        () ->
            s.getTable("Root")
                .getMetadata()
                .add(column("panels").setType(MODULE_ARRAY).setValues("Mod")),
        "Bare module name (no schema prefix) must be accepted, defaulting to current schema");

    db.clearCache();
    Schema reloaded = db.getSchema(schemaName);
    Column panels = reloaded.getTable("Root").getMetadata().getColumn("panels");

    assertNotNull(panels, "panels column must survive reload");
    assertEquals(MODULE_ARRAY, panels.getColumnType(), "column type must remain MODULE_ARRAY");
    assertNotNull(panels.getValues(), "values must survive reload");
    assertTrue(
        panels.getValues().contains("Mod"),
        "bare value must be stored AS-TYPED (not canonicalized to 'schema.Mod'), got: "
            + panels.getValues());
  }

  @Test
  void insertWithBareModuleValueActivatesModule() {
    Schema s = freshSchema("BareInsert");
    String schemaName = s.getMetadata().getName();

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));

    s.getTable("Root").getMetadata().add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    s.getTable("Root")
        .insert(row("id", "r1", "rootCol", "rootValue", "panels", "Mod", "modCol", "modValue"));

    List<Row> modRows = s.getTable("Mod").retrieveRows();
    assertEquals(
        1, modRows.size(), "Mod must have exactly one row (module activated by bare value)");
    assertEquals("r1", modRows.get(0).getString("id"), "Module row must share the root PK");
    assertEquals("modValue", modRows.get(0).getString("modCol"), "Module row must carry modCol");

    List<Row> queryRows = s.getTable("Root").query().select(s("id"), s("modCol")).retrieveRows();
    assertEquals(1, queryRows.size(), "Root query must return one row");
    assertEquals(
        "modValue",
        queryRows.get(0).getString("modCol"),
        "Module column must project via C4 query");
  }

  @Test
  void moduleArrayRejectsSchemaQualifiedValue() {
    Schema s = freshSchema("QualifiedDecl");
    String schemaName = s.getMetadata().getName();

    s.create(table("Root").add(column("id").setType(STRING).setPkey()));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Root")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues(schemaName + ".Mod")),
            "MODULE_ARRAY value with schema prefix must be rejected (bare table name only)");

    assertTrue(
        ex.getMessage().contains("bare table name") || ex.getMessage().contains("not supported"),
        "Error must describe bare-only requirement, got: " + ex.getMessage());
  }

  // ── C3 required-gating: inactive module's required column is never enforced ──

  @Test
  void requiredModuleColumnEnforcedOnlyWhenModuleActive() {
    Schema s = freshSchema("C3RequiredGate");

    s.create(table("Root").add(column("id").setPkey()));
    s.create(
        table("DiabetesPanel")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("hba1c").setRequired(true)));
    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("DiabetesPanel"));

    // arm 1: module active, required column omitted → rejected
    MolgenisException arm1Exception =
        assertThrows(
            MolgenisException.class,
            () -> s.getTable("Root").insert(row("id", "p1", "panels", "DiabetesPanel")),
            "Active module with missing required column must be rejected");
    assertTrue(
        arm1Exception.getMessage().contains("hba1c")
            || arm1Exception.getMessage().contains("required"),
        "Error must mention the required column or 'required', got: " + arm1Exception.getMessage());

    // arm 2: module active, required column provided → accepted
    assertDoesNotThrow(
        () ->
            s.getTable("Root").insert(row("id", "p2", "panels", "DiabetesPanel", "hba1c", "6.5%")),
        "Active module with required column supplied must succeed");
    List<Row> moduleRows = s.getTable("DiabetesPanel").retrieveRows();
    assertEquals(1, moduleRows.size(), "DiabetesPanel must have exactly one row after arm 2");
    assertEquals("p2", moduleRows.get(0).getString("id"), "Module row must share the root PK");

    // arm 3: module inactive (no panels), required column omitted → accepted (the gate)
    assertDoesNotThrow(
        () -> s.getTable("Root").insert(row("id", "p3")),
        "Inactive module must NOT enforce its required column — the gate must hold");
  }

  // ── C6: hard-delete deactivated module rows on update ───────────────────────

  @Test
  void updateRemovesDeactivatedModuleRow() {
    Schema s = freshSchema("C6Remove");

    s.create(table("Root").add(column("id").setPkey()));
    s.create(
        table("Mod1")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("col1").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("col2").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod1", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "1",
                "panels", new String[] {"Mod1", "Mod2"},
                "col1", "v1",
                "col2", "v2"));

    assertEquals(1, s.getTable("Mod1").retrieveRows().size(), "Mod1 must have a row after insert");
    assertEquals(1, s.getTable("Mod2").retrieveRows().size(), "Mod2 must have a row after insert");

    s.getTable("Root").update(row("id", "1", "panels", new String[] {"Mod1"}));

    assertEquals(1, s.getTable("Mod1").retrieveRows().size(), "Mod1 must still have its row");
    assertEquals(0, s.getTable("Mod2").retrieveRows().size(), "Mod2 row must be hard-deleted");
    assertEquals(1, s.getTable("Root").retrieveRows().size(), "Root row must be intact");
  }

  @Test
  void deactivatedModuleColumnProjectsNullAfterDelete() {
    Schema s = freshSchema("C6ProjNull");

    s.create(table("Root").add(column("id").setPkey()));
    s.create(
        table("Mod1")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("col1").setType(STRING)));
    s.create(
        table("Mod2")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("col2").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod1", "Mod2"));

    s.getTable("Root")
        .insert(
            row(
                "id", "1",
                "panels", new String[] {"Mod1", "Mod2"},
                "col1", "v1",
                "col2", "v2"));

    s.getTable("Root").update(row("id", "1", "panels", new String[] {"Mod1"}));

    List<Row> projected = s.getTable("Root").query().select(s("id"), s("col2")).retrieveRows();
    assertEquals(1, projected.size());
    assertNull(
        projected.get(0).getString("col2"),
        "col2 must be NULL after Mod2 deactivated (C4 row-presence gating)");
  }

  @Test
  void moduleExtendsModuleDeactivationRemovesFullChainInOrder() {
    Schema s = freshSchema("C6Chain");

    s.create(table("Root").add(column("id").setPkey()));
    s.create(
        table("ModA")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("colA").setType(STRING)));
    s.create(
        table("ModB")
            .setTableType(MODULE)
            .setInheritNames("ModA")
            .add(column("colB").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("ModA", "ModB"));

    s.getTable("Root")
        .insert(
            row(
                "id", "1",
                "panels", new String[] {"ModB"},
                "colA", "aVal",
                "colB", "bVal"));

    assertEquals(
        1, s.getTable("ModA").retrieveRows().size(), "ModA written via ancestor expansion");
    assertEquals(1, s.getTable("ModB").retrieveRows().size(), "ModB written directly");

    assertDoesNotThrow(
        () -> s.getTable("Root").update(row("id", "1", "panels", new String[0])),
        "Deactivating all modules must not throw FK-violation");

    assertEquals(0, s.getTable("ModA").retrieveRows().size(), "ModA row must be deleted");
    assertEquals(0, s.getTable("ModB").retrieveRows().size(), "ModB row must be deleted");
    assertEquals(1, s.getTable("Root").retrieveRows().size(), "Root row must be intact");
  }

  @Test
  void mixIsAAndModuleArrayDeactivationLeavesIsAIdentityIntact() {
    Schema s = freshSchema("C6Mix");

    s.create(table("RootData").add(column("id").setPkey()));
    s.create(table("Sub").setInheritNames("RootData").add(column("subCol").setType(STRING)));
    s.create(
        table("Mod1")
            .setTableType(MODULE)
            .setInheritNames("RootData")
            .add(column("mcol").setType(STRING)));

    s.getTable("RootData")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("Mod1"));

    s.getTable("Sub")
        .insert(row("id", "1", "subCol", "sv", "panels", new String[] {"Mod1"}, "mcol", "mv"));

    assertEquals(1, s.getTable("Mod1").retrieveRows().size(), "Mod1 must have a row");

    s.getTable("Sub").update(row("id", "1", "panels", new String[0]));

    assertEquals(0, s.getTable("Mod1").retrieveRows().size(), "Mod1 row must be hard-deleted");

    List<Row> subRows = s.getTable("Sub").retrieveRows();
    assertEquals(1, subRows.size(), "Sub (is-a) row must still exist");
    assertEquals("1", subRows.get(0).getString("id"), "Sub row id must be intact");

    String schemaName = s.getMetadata().getName();
    List<Row> rootRows =
        s.getTable("RootData").query().select(SelectColumn.s(MG_TABLECLASS)).retrieveRows();
    assertEquals(1, rootRows.size(), "RootData must have exactly one row");
    String tableclass = rootRows.get(0).getString(MG_TABLECLASS);
    String expectedTableclass = schemaName + ".Sub";
    assertEquals(
        expectedTableclass,
        tableclass,
        "mg_tableclass must remain the concrete is-a type after module deactivation");
  }

  // ── S1: MODULE_ARRAY must be declared on the root table, not a subtype ──────

  @Test
  void moduleArrayColumnMustBeDeclaredOnRoot() {
    Schema s = freshSchema("S1RootOnly");

    s.create(table("Root").add(column("id").setPkey()));
    s.create(table("Sub").setInheritNames("Root").add(column("subCol").setType(STRING)));
    s.create(table("Mod").setTableType(MODULE).setInheritNames("Root").add(column("modCol")));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Sub")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("Mod")),
            "MODULE_ARRAY declared on a non-root DATA subtype must be rejected");

    assertTrue(
        ex.getMessage().contains("root") || ex.getMessage().contains("subtype"),
        "Error must mention root/subtype constraint, got: " + ex.getMessage());
  }

  // ── S2: shared-ancestor module row kept when sibling deactivated ─────────────

  @Test
  void moduleExtendsModuleDeactivationKeepsSharedAncestorModule() {
    Schema s = freshSchema("S2SharedAncestor");

    s.create(table("Root").add(column("id").setPkey()));
    s.create(
        table("ModA")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("colA").setType(STRING)));
    s.create(
        table("ModB")
            .setTableType(MODULE)
            .setInheritNames("ModA")
            .add(column("colB").setType(STRING)));
    s.create(
        table("ModC")
            .setTableType(MODULE)
            .setInheritNames("ModA")
            .add(column("colC").setType(STRING)));

    s.getTable("Root")
        .getMetadata()
        .add(column("panels").setType(MODULE_ARRAY).setValues("ModA", "ModB", "ModC"));

    s.getTable("Root")
        .insert(
            row(
                "id", "1",
                "panels", new String[] {"ModB", "ModC"},
                "colA", "aVal",
                "colB", "bVal",
                "colC", "cVal"));

    assertEquals(1, s.getTable("ModA").retrieveRows().size(), "ModA written as shared ancestor");
    assertEquals(1, s.getTable("ModB").retrieveRows().size(), "ModB written directly");
    assertEquals(1, s.getTable("ModC").retrieveRows().size(), "ModC written directly");

    s.getTable("Root").update(row("id", "1", "panels", new String[] {"ModC"}));

    assertEquals(
        0,
        s.getTable("ModB").retrieveRows().size(),
        "ModB row must be gone: deactivated (not shared by remaining active modules)");
    assertEquals(
        1,
        s.getTable("ModA").retrieveRows().size(),
        "ModA row must remain: shared ancestor still required by active ModC");
    assertEquals(1, s.getTable("ModC").retrieveRows().size(), "ModC row must remain: still active");
    assertEquals(1, s.getTable("Root").retrieveRows().size(), "Root row must be intact");
  }

  @Test
  void insertRejectsDottedRowValueAgainstBareDeclaredColumn() {
    Schema s = freshSchema("DottedRowVal");
    String schemaName = s.getMetadata().getName();

    s.create(table("Root").add(column("id").setType(STRING).setPkey()).add(column("rootCol")));
    s.create(
        table("Mod")
            .setTableType(MODULE)
            .setInheritNames("Root")
            .add(column("modCol").setType(STRING)));

    s.getTable("Root").getMetadata().add(column("panels").setType(MODULE_ARRAY).setValues("Mod"));

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Root")
                    .insert(row("id", "r1", "rootCol", "v", "panels", schemaName + ".Mod")),
            "Inserting a dotted row value into a bare-declared MODULE_ARRAY column must be rejected");

    assertNotNull(ex.getMessage(), "Exception must have a message");
  }
}
