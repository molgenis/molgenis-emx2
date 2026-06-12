package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.Row.row;
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

    s.create(table("Mod").setTableType(MODULE).add(column("id").setPkey()).add(column("modCol")));

    s.create(
        table("Host")
            .add(column("id").setPkey())
            .add(
                column("panels")
                    .setType(MODULE_ARRAY)
                    .setValues(s.getMetadata().getName() + ".Mod")));

    db.clearCache();
    Schema reloaded = db.getSchema(s.getMetadata().getName());
    Column panels = reloaded.getTable("Host").getMetadata().getColumn("panels");

    assertNotNull(panels, "panels column must survive reload");
    assertEquals(MODULE_ARRAY, panels.getColumnType(), "column type must remain MODULE_ARRAY");
    assertNotNull(panels.getValues(), "values must survive reload");
    assertTrue(
        panels.getValues().contains(s.getMetadata().getName() + ".Mod"),
        "values must contain the MODULE reference after reload");

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
                    .add(
                        column("panels")
                            .setType(MODULE_ARRAY)
                            .setValues(s.getMetadata().getName() + ".DoesNotExist")));

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
                    .add(
                        column("panels")
                            .setType(MODULE_ARRAY)
                            .setValues(s.getMetadata().getName() + ".DataTable")));

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
                    .add(
                        column("panels")
                            .setType(MODULE_ARRAY)
                            .setValues(s.getMetadata().getName() + ".OntTable")));

    assertTrue(
        ex.getMessage().contains("OntTable") || ex.getMessage().contains("MODULE"),
        "Error must mention the wrong-type table or MODULE requirement, got: " + ex.getMessage());
  }

  // ── test 3c: malformed values (trailing dot, no dot) → MolgenisException not AIOOB ────────────

  @Test
  void moduleArrayValuesRejectMalformedQualifiedNames() {
    Schema s = freshSchema("Malformed");

    s.create(table("Host").add(column("id").setPkey()));

    MolgenisException trailingDot =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("schema.")),
            "Trailing-dot value must throw MolgenisException, not ArrayIndexOutOfBoundsException");
    assertTrue(
        trailingDot.getMessage().contains("schema-qualified")
            || trailingDot.getMessage().contains("schema."),
        "Error must describe the format requirement, got: " + trailingDot.getMessage());

    MolgenisException noDot =
        assertThrows(
            MolgenisException.class,
            () ->
                s.getTable("Host")
                    .getMetadata()
                    .add(column("panels").setType(MODULE_ARRAY).setValues("NoDotName")),
            "Value without dot must throw MolgenisException");
    assertTrue(
        noDot.getMessage().contains("schema-qualified") || noDot.getMessage().contains("NoDotName"),
        "Error must describe the format requirement, got: " + noDot.getMessage());
  }

  // ── test 4: INSERT element not in allowed set → rejected; allowed → accepted ──

  @Test
  void insertRejectsOutOfSetValueAndAcceptsInSetValue() {
    Schema s = freshSchema("Insert");
    String schemaName = s.getMetadata().getName();

    s.create(table("Mod1").setTableType(MODULE).add(column("id").setPkey()));
    s.create(table("Mod2").setTableType(MODULE).add(column("id").setPkey()));

    s.create(
        table("Host")
            .add(column("id").setPkey())
            .add(
                column("panels")
                    .setType(MODULE_ARRAY)
                    .setValues(schemaName + ".Mod1", schemaName + ".Mod2")));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("Host").insert(row("id", "r1", "panels", schemaName + ".UnknownModule")),
        "Inserting an element not in values must throw");

    assertDoesNotThrow(
        () -> s.getTable("Host").insert(row("id", "r2", "panels", schemaName + ".Mod1")),
        "Inserting an element in values must succeed");
  }

  // ── test 5: UPDATE to out-of-set value → rejected ───────────────────────────

  @Test
  void updateRejectsOutOfSetValue() {
    Schema s = freshSchema("Update");
    String schemaName = s.getMetadata().getName();

    s.create(table("ModU").setTableType(MODULE).add(column("id").setPkey()));

    s.create(
        table("HostU")
            .add(column("id").setPkey())
            .add(column("panels").setType(MODULE_ARRAY).setValues(schemaName + ".ModU")));

    s.getTable("HostU").insert(row("id", "u1", "panels", schemaName + ".ModU"));

    assertThrows(
        MolgenisException.class,
        () -> s.getTable("HostU").update(row("id", "u1", "panels", schemaName + ".NotAllowed")),
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

    s.create(table("PanelA").setTableType(MODULE).add(column("id").setPkey()));
    s.create(table("PanelB").setTableType(MODULE).add(column("id").setPkey()));

    s.create(
        table("DogWithPanels")
            .setInheritNames("Dog")
            .add(column("extra"))
            .add(
                column("axis1")
                    .setType(MODULE_ARRAY)
                    .setValues(schemaName + ".PanelA", schemaName + ".PanelB"))
            .add(column("axis2").setType(MODULE_ARRAY).setValues(schemaName + ".PanelA")));

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
}
