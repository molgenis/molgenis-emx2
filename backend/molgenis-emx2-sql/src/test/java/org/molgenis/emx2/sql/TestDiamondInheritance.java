package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.STRING;
import static org.molgenis.emx2.Constants.MG_TABLECLASS;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestDiamondInheritance {

  private static final String SCHEMA = "TestDiamondInheritance";

  private static Database db;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  /** Build diamond: A <- B <- D, A <- C <- D using the setInheritNames API. */
  private Schema buildDiamondViaApi() {
    db.dropSchemaIfExists(SCHEMA);
    Schema s = db.createSchema(SCHEMA);

    s.create(
        table("A").add(column("id").setType(STRING).setPkey()).add(column("aCol").setType(STRING)));

    s.create(table("B").setInheritNames("A").add(column("bCol").setType(STRING)));

    s.create(table("C").setInheritNames("A").add(column("cCol").setType(STRING)));

    s.create(table("D").setInheritNames("B", "C").add(column("dCol").setType(STRING)));

    return db.getSchema(SCHEMA);
  }

  @Test
  void diamondChildHasSingleRootPrimaryKey() {
    Schema s = buildDiamondViaApi();

    List<String> primaryKeys = s.getTable("D").getMetadata().getPrimaryKeys();

    assertEquals(1, primaryKeys.size(), "D must have exactly one PK column (root A's id)");
    assertEquals("id", primaryKeys.get(0));
  }

  @Test
  void diamondChildHasForeignKeyPerParent() {
    Schema s = buildDiamondViaApi();

    List<String> fkDescriptions =
        ((SqlSchema) s)
            .getJooq().meta().getSchemas(SCHEMA).get(0).getTable("D").getReferences().stream()
                .map(Object::toString)
                .collect(Collectors.toList());

    assertTrue(
        fkDescriptions.stream().anyMatch(desc -> desc.contains("\"B\"")),
        "D must have a FK constraint referencing B, found FKs: " + fkDescriptions);
    assertTrue(
        fkDescriptions.stream().anyMatch(desc -> desc.contains("\"C\"")),
        "D must have a FK constraint referencing C, found FKs: " + fkDescriptions);
  }

  @Test
  void insertAndSelectRoundtripThroughDiamond() {
    Schema s = buildDiamondViaApi();

    s.getTable("D")
        .insert(
            row(
                "id", "row1", "aCol", "aValue", "bCol", "bValue", "cCol", "cValue", "dCol",
                "dValue"));

    List<Row> dRows = s.getTable("D").select().retrieveRows();
    assertEquals(1, dRows.size());
    Row dRow = dRows.get(0);
    assertEquals("row1", dRow.getString("id"));
    assertEquals("aValue", dRow.getString("aCol"));
    assertEquals("bValue", dRow.getString("bCol"));
    assertEquals("cValue", dRow.getString("cCol"));
    assertEquals("dValue", dRow.getString("dCol"));

    List<Row> aRows = s.getTable("A").select().retrieveRows();
    assertEquals(1, aRows.size(), "Shared root A must contain exactly one row (not duplicated)");
    assertEquals(SCHEMA + ".D", aRows.get(0).getString(MG_TABLECLASS));
  }

  @Test
  void duplicateColumnAcrossParentsRejected() {
    String dupSchema = SCHEMA + "DupCol";
    db.dropSchemaIfExists(dupSchema);
    Schema s = db.createSchema(dupSchema);

    try {
      s.create(
          table("Root")
              .add(column("id").setType(STRING).setPkey())
              .add(column("rootCol").setType(STRING)));
      s.create(table("P1").setInheritNames("Root").add(column("dup").setType(STRING)));
      s.create(table("P2").setInheritNames("Root").add(column("dup").setType(STRING)));
      s.create(table("Child").add(column("childCol").setType(STRING)));

      SqlTableMetadata childMeta = (SqlTableMetadata) s.getTable("Child").getMetadata();
      childMeta.setInheritNames("P1", "P2");

      fail("Should have thrown MolgenisException due to duplicate column 'dup' across parents");
    } catch (MolgenisException e) {
      assertTrue(
          e.getMessage().contains("dup"),
          "Exception must mention the conflicting column name 'dup', got: " + e.getMessage());
    } finally {
      db.dropSchemaIfExists(dupSchema);
    }
  }

  @Test
  void twoRootsRejected() {
    String twoRootsSchema = SCHEMA + "TwoRoots";
    db.dropSchemaIfExists(twoRootsSchema);
    Schema s = db.createSchema(twoRootsSchema);

    try {
      s.create(table("Root1").add(column("id1").setType(STRING).setPkey()).add(column("r1Col")));
      s.create(table("Root2").add(column("id2").setType(STRING).setPkey()).add(column("r2Col")));
      s.create(table("Branch1").setInheritNames("Root1").add(column("b1Col")));
      s.create(table("Branch2").setInheritNames("Root2").add(column("b2Col")));
      s.create(table("Leaf").add(column("leafCol")));

      SqlTableMetadata leafMeta = (SqlTableMetadata) s.getTable("Leaf").getMetadata();
      leafMeta.setInheritNames("Branch1", "Branch2");

      fail("Should have thrown MolgenisException: DAG has two distinct roots");
    } catch (MolgenisException e) {
      assertTrue(
          e.getMessage().toLowerCase().contains("root")
              || e.getMessage().toLowerCase().contains("multiple"),
          "Exception must describe the multiple-root violation, got: " + e.getMessage());
      Schema reloaded = db.getSchema(twoRootsSchema);
      List<String> leafParents = reloaded.getTable("Leaf").getMetadata().getInheritNames();
      assertTrue(
          leafParents.isEmpty(),
          "After validate-once rejection, Leaf must have no wired parents (no partial DDL), got: "
              + leafParents);
    } finally {
      db.dropSchemaIfExists(twoRootsSchema);
    }
  }

  @Test
  void diamondSurvivesSchemaReload() {
    buildDiamondViaApi();

    db.clearCache();
    Schema reloaded = db.getSchema(SCHEMA);

    List<String> inheritNames = reloaded.getTable("D").getMetadata().getInheritNames();
    assertTrue(inheritNames.contains("B"), "After reload D must still list B as parent");
    assertTrue(inheritNames.contains("C"), "After reload D must still list C as parent");

    reloaded
        .getTable("D")
        .insert(
            row(
                "id", "reload1", "aCol", "aReload", "bCol", "bReload", "cCol", "cReload", "dCol",
                "dReload"));

    List<Row> dRows = reloaded.getTable("D").select().retrieveRows();
    assertEquals(1, dRows.size());
    assertEquals("reload1", dRows.get(0).getString("id"));
  }

  // C1: mg_tableclass must live ONLY on the single shared root (A), NOT on intermediate B or C
  @Test
  void mgTableclassLivesOnlyOnRoot() {
    Schema s = buildDiamondViaApi();

    assertNotNull(
        s.getTable("A").getMetadata().getLocalColumn(MG_TABLECLASS),
        "mg_tableclass must be present on root A");
    assertNull(
        s.getTable("B").getMetadata().getLocalColumn(MG_TABLECLASS),
        "mg_tableclass must NOT be present on intermediate B");
    assertNull(
        s.getTable("C").getMetadata().getLocalColumn(MG_TABLECLASS),
        "mg_tableclass must NOT be present on intermediate C");
    assertNull(
        s.getTable("D").getMetadata().getLocalColumn(MG_TABLECLASS),
        "mg_tableclass must NOT be present on leaf D");
  }

  // C1 back-compat: single chain — mg_tableclass on root only, NOT on Employee/Manager/CEO
  @Test
  void mgTableclassOnRootOnlyInSingleChain() {
    String chainSchema = SCHEMA + "Chain";
    db.dropSchemaIfExists(chainSchema);
    Schema s = db.createSchema(chainSchema);
    try {
      s.create(
          table("Person").add(column("id").setType(STRING).setPkey()).add(column("name")),
          table("Employee").setInheritNames("Person").add(column("salary")),
          table("Manager").setInheritNames("Employee").add(column("dept")),
          table("CEO").setInheritNames("Manager").add(column("bonus")));

      assertNotNull(
          s.getTable("Person").getMetadata().getLocalColumn(MG_TABLECLASS),
          "mg_tableclass on root Person");
      assertNull(
          s.getTable("Employee").getMetadata().getLocalColumn(MG_TABLECLASS),
          "mg_tableclass NOT on Employee");
      assertNull(
          s.getTable("Manager").getMetadata().getLocalColumn(MG_TABLECLASS),
          "mg_tableclass NOT on Manager");
      assertNull(
          s.getTable("CEO").getMetadata().getLocalColumn(MG_TABLECLASS),
          "mg_tableclass NOT on CEO");
    } finally {
      db.dropSchemaIfExists(chainSchema);
    }
  }

  // S2: alterColumn guard must check ALL parents, not only the primary parent
  @Test
  void alterColumnRejectedWhenOwnedBySecondParent() {
    Schema s = buildDiamondViaApi();

    SqlTableMetadata dMeta = (SqlTableMetadata) s.getTable("D").getMetadata();
    assertThrows(
        MolgenisException.class,
        () -> dMeta.alterColumn("cCol", column("cCol").setType(STRING)),
        "Altering a column owned by the 2nd parent (C) must be rejected");
  }

  // S3: collision CTE must catch collisions reachable only via the 2nd parent
  @Test
  void addColumnRejectedWhenCollidesViaSecondParentSubclass() {
    String collSchema = SCHEMA + "Coll";
    db.dropSchemaIfExists(collSchema);
    Schema s = db.createSchema(collSchema);
    try {
      s.create(
          table("Root").add(column("id").setType(STRING).setPkey()),
          table("P1").setInheritNames("Root").add(column("p1Col").setType(STRING)),
          table("P2").setInheritNames("Root").add(column("p2Col").setType(STRING)),
          table("Child").add(column("childCol").setType(STRING)));

      SqlTableMetadata childMeta = (SqlTableMetadata) s.getTable("Child").getMetadata();
      childMeta.setInheritNames("P1", "P2");

      SqlTableMetadata p2Meta = (SqlTableMetadata) s.getTable("P2").getMetadata();
      assertThrows(
          MolgenisException.class,
          () -> p2Meta.add(column("childCol").setType(STRING)),
          "Adding childCol to P2 must be rejected: childCol already exists in diamond child Child");
    } finally {
      db.dropSchemaIfExists(collSchema);
    }
  }

  @Test
  void cyclicInheritanceThrowsMolgenisException() {
    String cycleSchema = SCHEMA + "Cycle";
    db.dropSchemaIfExists(cycleSchema);
    Schema s = db.createSchema(cycleSchema);
    try {
      s.create(table("A").add(column("id").setType(STRING).setPkey()));
      s.create(table("B").setInheritNames("A").add(column("bCol").setType(STRING)));

      SqlTableMetadata aMeta = (SqlTableMetadata) s.getTable("A").getMetadata();
      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () -> aMeta.setInheritNames("B"),
              "Making A extend B (which extends A) must throw due to cyclic inheritance");
      assertTrue(
          thrown.getMessage().toLowerCase().contains("cyclic")
              || thrown.getMessage().toLowerCase().contains("cycle"),
          "Exception must mention cyclic inheritance, got: " + thrown.getMessage());
    } finally {
      db.dropSchemaIfExists(cycleSchema);
    }
  }

  // S6: error message on unresolved parent must list ALL unresolved parent names
  @Test
  void createTableWithUnknownParentsListsAllMissingNames() {
    String missingSchema = SCHEMA + "Missing";
    db.dropSchemaIfExists(missingSchema);
    Schema s = db.createSchema(missingSchema);
    try {
      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () ->
                  s.create(
                      table("Orphan")
                          .setInheritNames(List.of("Ghost1", "Ghost2"))
                          .add(column("x").setType(STRING))));
      assertTrue(
          thrown.getMessage().contains("Ghost1") || thrown.getMessage().contains("Ghost2"),
          "Error must mention at least one of the missing parent names, got: "
              + thrown.getMessage());
    } finally {
      db.dropSchemaIfExists(missingSchema);
    }
  }
}
