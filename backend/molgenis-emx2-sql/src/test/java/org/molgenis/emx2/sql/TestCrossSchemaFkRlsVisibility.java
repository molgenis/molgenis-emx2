package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.SelectColumn.s;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestCrossSchemaFkRlsVisibility {

  private static final String SCHEMA_A_NAME = "TestCsFkRlsA";
  private static final String SCHEMA_B_NAME = "TestCsFkRlsB";

  private static final String TABLE_PARENT = "Parent";
  private static final String TABLE_CHILD = "Child";
  private static final String TABLE_CHILD_ARR = "ChildArr";
  private static final String TABLE_SAMPLE = "Sample";

  private static final String ROLE_OWN_READER = "own-reader";
  private static final String GROUP_A = "groupA";

  private static final String USER_U1 = "TcsFkRlsU1";
  private static final String USER_U2 = "TcsFkRlsU2";

  private static final String MG_USER_U1 = "MG_USER_" + USER_U1;
  private static final String MG_USER_U2 = "MG_USER_" + USER_U2;

  private static final String ROW_P1 = "P1";
  private static final String ROW_P2 = "P2";
  private static final String ROW_C1 = "C1";
  private static final String ROW_C2 = "C2";
  private static final String ROW_C_ARR = "CArr";
  private static final String ROW_SAMPLE_U2 = "SampleU2";

  private static Database db;
  private static DSLContext jooq;
  private static SqlRoleManager roleManager;
  private static Schema schemaA;
  private static Schema schemaB;

  @BeforeAll
  static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
    jooq = ((SqlDatabase) db).getJooq();
    roleManager = new SqlRoleManager((SqlDatabase) db);

    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_B_NAME);
    db.dropSchemaIfExists(SCHEMA_A_NAME);

    schemaA = db.createSchema(SCHEMA_A_NAME);
    schemaB = db.createSchema(SCHEMA_B_NAME);

    if (!db.hasUser(USER_U1)) db.addUser(USER_U1);
    if (!db.hasUser(USER_U2)) db.addUser(USER_U2);

    schemaA.create(table(TABLE_PARENT).add(column("id").setPkey()));
    schemaA.create(table(TABLE_SAMPLE).setInheritName(TABLE_PARENT).add(column("label")));

    SqlTableMetadata parentMeta = (SqlTableMetadata) schemaA.getTable(TABLE_PARENT).getMetadata();
    parentMeta.setRlsEnabled(true);

    schemaB.create(
        table(TABLE_CHILD)
            .add(column("id").setPkey())
            .add(
                column("parent")
                    .setType(REF)
                    .setRefSchemaName(SCHEMA_A_NAME)
                    .setRefTable(TABLE_PARENT)));

    schemaB.create(
        table(TABLE_CHILD_ARR)
            .add(column("id").setPkey())
            .add(
                column("parents")
                    .setType(REF_ARRAY)
                    .setRefSchemaName(SCHEMA_A_NAME)
                    .setRefTable(TABLE_PARENT)));

    roleManager.createGroup(schemaA, GROUP_A);
    roleManager.createRole(SCHEMA_A_NAME, ROLE_OWN_READER);

    PermissionSet ps = new PermissionSet();
    TablePermission tp = new TablePermission(TABLE_PARENT);
    tp.setSelect(SelectScope.OWN);
    tp.setInsert(UpdateScope.NONE);
    tp.setUpdate(UpdateScope.NONE);
    tp.setDelete(UpdateScope.NONE);
    ps.putTable(TABLE_PARENT, tp);

    TablePermission tpSample = new TablePermission(TABLE_SAMPLE);
    tpSample.setSelect(SelectScope.OWN);
    tpSample.setInsert(UpdateScope.NONE);
    tpSample.setUpdate(UpdateScope.NONE);
    tpSample.setDelete(UpdateScope.NONE);
    ps.putTable(TABLE_SAMPLE, tpSample);

    roleManager.setPermissions(schemaA, ROLE_OWN_READER, ps);
    roleManager.addGroupMembership(SCHEMA_A_NAME, GROUP_A, USER_U1, ROLE_OWN_READER);

    schemaB.addMember(USER_U1, "Editor");

    jooq.execute(
        "INSERT INTO \""
            + SCHEMA_A_NAME
            + "\".\""
            + TABLE_PARENT
            + "\" (id, mg_owner) VALUES (?, ?)",
        ROW_P1,
        MG_USER_U1);
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA_A_NAME
            + "\".\""
            + TABLE_PARENT
            + "\" (id, mg_owner) VALUES (?, ?)",
        ROW_P2,
        MG_USER_U2);

    jooq.execute(
        "INSERT INTO \""
            + SCHEMA_A_NAME
            + "\".\""
            + TABLE_PARENT
            + "\" (id, mg_owner) VALUES (?, ?)",
        ROW_SAMPLE_U2,
        MG_USER_U2);
    jooq.execute(
        "INSERT INTO \""
            + SCHEMA_A_NAME
            + "\".\""
            + TABLE_SAMPLE
            + "\" (id, label, mg_owner) VALUES (?, ?, ?)",
        ROW_SAMPLE_U2,
        "u2-sample",
        MG_USER_U2);

    schemaB.getTable(TABLE_CHILD).insert(new Row().set("id", ROW_C1).set("parent", ROW_P1));
    schemaB.getTable(TABLE_CHILD).insert(new Row().set("id", ROW_C2).set("parent", ROW_P2));

    schemaB
        .getTable(TABLE_CHILD_ARR)
        .insert(new Row().set("id", ROW_C_ARR).set("parents", new String[] {ROW_P1, ROW_P2}));
  }

  @AfterAll
  static void tearDown() {
    db.becomeAdmin();
    db.dropSchemaIfExists(SCHEMA_B_NAME);
    db.dropSchemaIfExists(SCHEMA_A_NAME);
  }

  @Test
  void joinResolvesOnlyVisibleParents() {
    db.setActiveUser(USER_U1);
    try {
      String json =
          schemaB.getTable(TABLE_CHILD).select(s("id"), s("parent", s("id"))).retrieveJSON();
      assertTrue(json.contains(ROW_C1), "C1 must appear in result");
      assertTrue(json.contains(ROW_P1), "P1 must be resolved for C1");
      assertTrue(json.contains(ROW_C2), "C2 row must appear (Child is fully readable by U1)");
      assertFalse(json.contains(ROW_P2), "P2 must NOT appear — filtered by Parent RLS for U1");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void scalarRefProjectsNullForInvisibleParent() {
    db.setActiveUser(USER_U1);
    try {
      List<Row> rows = schemaB.getTable(TABLE_CHILD).retrieveRows();
      Row c1Row =
          rows.stream().filter(r -> ROW_C1.equals(r.getString("id"))).findFirst().orElse(null);
      Row c2Row =
          rows.stream().filter(r -> ROW_C2.equals(r.getString("id"))).findFirst().orElse(null);
      assertNotNull(c1Row, "C1 must be returned");
      assertNotNull(c2Row, "C2 must be returned (Child has no RLS)");
      assertEquals(ROW_P1, c1Row.getString("parent"), "C1.parent FK must resolve to P1");
      assertNull(
          c2Row.getString("parent"), "C2.parent FK must be clamped to null by SqlQuery auto-join");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void refArrayDropsInvisibleElements() {
    db.setActiveUser(USER_U1);
    try {
      String json =
          schemaB.getTable(TABLE_CHILD_ARR).select(s("id"), s("parents", s("id"))).retrieveJSON();
      assertTrue(json.contains(ROW_P1), "P1 must appear in resolved REF_ARRAY");
      assertFalse(json.contains(ROW_P2), "P2 must be dropped from REF_ARRAY by RLS");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void refbackEmptyForInvisibleParent() {
    db.setActiveUser(USER_U1);
    try {
      List<Row> parents = schemaA.getTable(TABLE_PARENT).retrieveRows();
      List<String> parentIds = parents.stream().map(r -> r.getString("id")).toList();
      assertEquals(1, parents.size(), "U1 must see only P1 via RLS");
      assertTrue(parentIds.contains(ROW_P1), "P1 must be visible");
      assertFalse(parentIds.contains(ROW_P2), "P2 must be invisible — owned by U2");
    } finally {
      db.becomeAdmin();
    }
  }

  @Test
  void inheritanceCompositionFiltersChildSubclass() {
    db.setActiveUser(USER_U1);
    try {
      List<Row> parents = schemaA.getTable(TABLE_PARENT).retrieveRows();
      List<String> ids = parents.stream().map(r -> r.getString("id")).toList();
      assertFalse(
          ids.contains(ROW_SAMPLE_U2), "U1 must not see Sample row owned by U2 via Parent query");
      assertFalse(ids.contains(ROW_P2), "U1 must not see P2 owned by U2");
      assertTrue(ids.contains(ROW_P1), "U1 must see P1 owned by themselves");
    } finally {
      db.becomeAdmin();
    }
  }
}
