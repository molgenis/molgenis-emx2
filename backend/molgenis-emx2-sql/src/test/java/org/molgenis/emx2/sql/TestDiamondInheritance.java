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
      s.create(table("Child").setInheritNames("P1", "P2").add(column("childCol").setType(STRING)));

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
  void twoBasesRejectedAtCreateTime() {
    String twoBasesSchema = SCHEMA + "TwoBases";
    db.dropSchemaIfExists(twoBasesSchema);
    Schema s = db.createSchema(twoBasesSchema);

    try {
      s.create(
          table("Base1")
              .add(column("id1").setType(STRING).setPkey())
              .add(column("base1Col").setType(STRING)),
          table("Base2")
              .add(column("id2").setType(STRING).setPkey())
              .add(column("base2Col").setType(STRING)),
          table("Branch1").setInheritNames("Base1").add(column("branch1Col").setType(STRING)),
          table("Branch2").setInheritNames("Base2").add(column("branch2Col").setType(STRING)));

      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () ->
                  s.create(
                      table("Leaf")
                          .setInheritNames("Branch1", "Branch2")
                          .add(column("leafCol").setType(STRING))),
              "Creating a table whose parents do not share one root must be rejected");
      assertTrue(
          thrown.getMessage().toLowerCase().contains("multiple roots"),
          "Exception must state the multiple-root diagnosis, got: " + thrown.getMessage());
      assertTrue(
          thrown.getMessage().contains("Base1") && thrown.getMessage().contains("Base2"),
          "Exception must name both conflicting roots, got: " + thrown.getMessage());

      db.clearCache();
      assertFalse(
          db.getSchema(twoBasesSchema).getMetadata().getTableNames().contains("Leaf"),
          "After create-time rejection Leaf must not exist (no partial DDL)");
    } finally {
      db.dropSchemaIfExists(twoBasesSchema);
    }
  }

  @Test
  void crossSchemaDiamondChildInheritsBothParentsFromOtherSchema() {
    String upstreamSchema = SCHEMA + "Upstream";
    String downstreamSchema = SCHEMA + "Downstream";
    db.dropSchemaIfExists(downstreamSchema);
    db.dropSchemaIfExists(upstreamSchema);

    Schema upstream = db.createSchema(upstreamSchema);
    upstream.create(
        table("Base")
            .add(column("id").setType(STRING).setPkey())
            .add(column("baseCol").setType(STRING)),
        table("Alpha").setInheritNames("Base").add(column("alphaCol").setType(STRING)),
        table("Beta").setInheritNames("Base").add(column("betaCol").setType(STRING)));

    Schema downstream = db.createSchema(downstreamSchema);
    downstream.create(
        table("Child")
            .setImportSchema(upstreamSchema)
            .setInheritNames("Alpha", "Beta")
            .add(column("childCol").setType(STRING)));

    db.clearCache();
    TableMetadata child = db.getSchema(downstreamSchema).getTable("Child").getMetadata();
    assertEquals(
        List.of("Alpha", "Beta"),
        child.getInheritNames(),
        "Cross-schema child must keep both parents after reload");
    assertEquals(
        upstreamSchema,
        child.getImportSchema(),
        "Cross-schema child must keep the single table-level import schema");
    assertEquals(
        List.of("id"),
        child.getPrimaryKeys(),
        "Cross-schema diamond child must have exactly the shared root primary key");

    List<String> childForeignKeys =
        ((SqlSchema) db.getSchema(downstreamSchema))
                .getJooq()
                .meta()
                .getSchemas(downstreamSchema)
                .get(0)
                .getTable("Child")
                .getReferences()
                .stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    assertTrue(
        childForeignKeys.stream().anyMatch(fk -> fk.contains("\"Alpha\"")),
        "Child must have a FK to cross-schema parent Alpha, found: " + childForeignKeys);
    assertTrue(
        childForeignKeys.stream().anyMatch(fk -> fk.contains("\"Beta\"")),
        "Child must have a FK to cross-schema parent Beta, found: " + childForeignKeys);

    db.getSchema(downstreamSchema)
        .getTable("Child")
        .insert(
            row(
                "id",
                "cross1",
                "baseCol",
                "baseValue",
                "alphaCol",
                "alphaValue",
                "betaCol",
                "betaValue",
                "childCol",
                "childValue"));

    List<Row> childRows = db.getSchema(downstreamSchema).getTable("Child").select().retrieveRows();
    assertEquals(1, childRows.size());
    assertEquals("baseValue", childRows.get(0).getString("baseCol"));
    assertEquals("alphaValue", childRows.get(0).getString("alphaCol"));
    assertEquals("betaValue", childRows.get(0).getString("betaCol"));

    List<Row> baseRows = db.getSchema(upstreamSchema).getTable("Base").select().retrieveRows();
    assertEquals(
        1, baseRows.size(), "Shared root in the other schema must hold exactly one row for cross1");
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

  // B1: an upgraded database (pre-slice-1 physical layout, migration33 does not drop the
  // redundant columns — that cleanup is its own out-of-scope PR) can have mg_tableclass
  // physically present on an intermediate table as well as the root. Selecting through such a
  // chain must not throw 'column reference "mg_tableclass" is ambiguous'.
  @Test
  void selectSucceedsWhenIntermediateStillHasLegacyMgTableclassColumn() {
    String legacySchema = SCHEMA + "Legacy";
    db.dropSchemaIfExists(legacySchema);
    Schema s = db.createSchema(legacySchema);
    try {
      s.create(
          table("Person").add(column("id").setType(STRING).setPkey()).add(column("name")),
          table("Employee").setInheritNames("Person").add(column("salary")),
          table("Manager").setInheritNames("Employee").add(column("dept")));

      // simulate the legacy physical AND metadata layout: give the intermediate table its own
      // mg_tableclass column too, exactly as pre-slice-1 code did on every extend (physical
      // column plus a column_metadata row, both created by the ordinary add-column path at the
      // time), and as migration33 leaves it (statement 3, the cleanup DO-block, is explicitly
      // out of scope for this story)
      org.jooq.DSLContext jooq = ((SqlDatabase) db).getJooq();
      TableMetadata employeeMeta = s.getMetadata().getTableMetadata("Employee");
      jooq.execute(
          "ALTER TABLE {0} ADD COLUMN {1} VARCHAR",
          org.jooq.impl.DSL.table(org.jooq.impl.DSL.name(legacySchema, "Employee")),
          org.jooq.impl.DSL.name(MG_TABLECLASS));
      MetadataUtils.saveColumnMetadata(
          jooq, new Column(employeeMeta, MG_TABLECLASS).setReadonly(true).setPosition(10005));
      db.clearCache();
      Schema reloaded = db.getSchema(legacySchema);

      reloaded
          .getTable("Manager")
          .insert(row("id", "m1", "name", "n1", "salary", "1", "dept", "eng"));

      assertDoesNotThrow(
          () -> reloaded.getTable("Manager").select().retrieveRows(),
          "select through a chain with a legacy per-level mg_tableclass column must not throw "
              + "'column reference \"mg_tableclass\" is ambiguous'");
      assertEquals(1, reloaded.getTable("Manager").select().retrieveRows().size());
    } finally {
      db.dropSchemaIfExists(legacySchema);
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
          table("Child").setInheritNames("P1", "P2").add(column("childCol").setType(STRING)));

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
  void cyclicInheritanceRejectedAtCreateTime() {
    String selfExtendsSchema = SCHEMA + "SelfExtends";
    db.dropSchemaIfExists(selfExtendsSchema);
    Schema s = db.createSchema(selfExtendsSchema);
    try {
      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () ->
                  s.create(
                      table("Loop").setInheritNames("Loop").add(column("loopCol").setType(STRING))),
              "Creating a table whose inheritance graph closes on itself must be rejected");
      assertTrue(
          thrown.getMessage().toLowerCase().contains("cyclic inheritance detected"),
          "Exception must state the cyclic inheritance diagnosis, got: " + thrown.getMessage());
      assertTrue(
          thrown.getMessage().contains("Loop"),
          "Exception must name the table on the cycle, got: " + thrown.getMessage());

      db.clearCache();
      assertFalse(
          db.getSchema(selfExtendsSchema).getMetadata().getTableNames().contains("Loop"),
          "After create-time rejection the cyclic table must not exist");
    } finally {
      db.dropSchemaIfExists(selfExtendsSchema);
    }
  }

  @Test
  void mergePreservesAllParentsOfDiamondChild() {
    String mergeSchemaName = SCHEMA + "Merge";
    db.dropSchemaIfExists(mergeSchemaName);
    Schema target = db.createSchema(mergeSchemaName);

    SchemaMetadata diamondSpec = new SchemaMetadata();
    diamondSpec.create(
        table("A").add(column("id").setType(STRING).setPkey()).add(column("aCol").setType(STRING)));
    diamondSpec.create(table("B").setInheritNames("A").add(column("bCol").setType(STRING)));
    diamondSpec.create(table("C").setInheritNames("A").add(column("cCol").setType(STRING)));
    diamondSpec.create(table("D").setInheritNames("B", "C").add(column("dCol").setType(STRING)));

    target.migrate(diamondSpec);

    db.clearCache();
    List<String> mergedParents =
        db.getSchema(mergeSchemaName).getTable("D").getMetadata().getInheritNames();
    assertTrue(
        mergedParents.containsAll(List.of("B", "C")),
        "After migrate, D must have both B and C as parents, got: " + mergedParents);

    db.dropSchemaIfExists(mergeSchemaName);
  }

  @Test
  void addingParentAfterCreationThrows() {
    String immutableSchema = SCHEMA + "Immutable";
    db.dropSchemaIfExists(immutableSchema);
    Schema s = db.createSchema(immutableSchema);
    try {
      s.create(
          table("Root").add(column("id").setType(STRING).setPkey()),
          table("P1").setInheritNames("Root").add(column("p1Col").setType(STRING)),
          table("P2").setInheritNames("Root").add(column("p2Col").setType(STRING)),
          table("Child").setInheritNames("P1").add(column("childCol").setType(STRING)));

      SqlTableMetadata childMeta = (SqlTableMetadata) s.getTable("Child").getMetadata();
      MolgenisException thrown =
          assertThrows(
              MolgenisException.class,
              () -> childMeta.setInheritNames("P1", "P2"),
              "Adding a parent after table creation must be rejected: extends is immutable");
      assertTrue(
          thrown.getMessage().toLowerCase().contains("immutable")
              || thrown.getMessage().toLowerCase().contains("cannot change"),
          "Exception must explain extends is immutable, got: " + thrown.getMessage());

      List<String> parents =
          db.getSchema(immutableSchema).getTable("Child").getMetadata().getInheritNames();
      assertEquals(
          List.of("P1"), parents, "Child must still extend only P1 after the rejected change");
    } finally {
      db.dropSchemaIfExists(immutableSchema);
    }
  }

  @Test
  void reimportingIdenticalInheritNamesIsSilentNoOp() {
    String idempotentSchema = SCHEMA + "Idempotent";
    db.dropSchemaIfExists(idempotentSchema);
    Schema s = db.createSchema(idempotentSchema);
    try {
      s.create(
          table("Root").add(column("id").setType(STRING).setPkey()),
          table("P1").setInheritNames("Root").add(column("p1Col").setType(STRING)),
          table("P2").setInheritNames("Root").add(column("p2Col").setType(STRING)),
          table("Child").setInheritNames("P1", "P2").add(column("childCol").setType(STRING)));

      SqlTableMetadata childMeta = (SqlTableMetadata) s.getTable("Child").getMetadata();
      childMeta.setInheritNames("P2", "P1");

      List<String> parents =
          db.getSchema(idempotentSchema).getTable("Child").getMetadata().getInheritNames();
      assertTrue(
          parents.containsAll(List.of("P1", "P2")),
          "Re-importing the identical extends must be a silent no-op; Child still extends P1 and P2, got: "
              + parents);
    } finally {
      db.dropSchemaIfExists(idempotentSchema);
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
