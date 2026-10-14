package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.FILE;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestChangelog {

  private static Database database;

  @BeforeAll
  static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void testChangelog() {
    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          Schema schema = setupSchemaWithTestDataAndChangelog(db, "testSchemaChanges");
          assertEquals(3, schema.getChanges(100).size());
          assertEquals('I', schema.getChanges(100).get(0).operation());
          assertEquals("Person", schema.getChanges(100).get(0).tableName());
        });
  }

  @Test
  void testChangelogLimit() {
    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          Schema schema = setupSchemaWithTestDataAndChangelog(db, "testSchemaChangesWithLimit");
          List<Change> changes = schema.getChanges(1);
          assertEquals(1, changes.size());
          assertEquals("Person", changes.get(0).tableName());
          assertEquals('I', changes.get(0).operation());
          String newRow = changes.get(0).newRowData();
          assertTrue(newRow.contains("Kwik") || newRow.contains("Kwek") || newRow.contains("Kwak"));
        });
  }

  @Test
  void testChangelogOffset() {
    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          Schema schema = setupSchemaWithTestDataAndChangelog(db, "testSchemaChangesWithOffset");
          List<Change> changes = schema.getChanges(1, 1);
          assertEquals(1, changes.size());
          assertEquals("Person", changes.get(0).tableName());
          assertEquals('I', changes.get(0).operation());
          String newRow = changes.get(0).newRowData();
          assertTrue(newRow.contains("Kwik") || newRow.contains("Kwek") || newRow.contains("Kwak"));
        });
  }

  @Test
  void testGetChangesCount() {
    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          Schema schema = setupSchemaWithTestDataAndChangelog(db, "testSchemaChangesChangeCount");
          assertEquals(java.util.Optional.of(3), java.util.Optional.of(schema.getChangesCount()));
        });
  }

  @Test
  void lastUpdateBranchReadsStampIndex() {
    Schema schema = setupChangelogSchema("TestChangelogIndex");
    Table table = schema.create(table("Item", column("id").setType(INT).setPkey()));
    List<Row> rows = new ArrayList<>();
    for (int i = 0; i < 3000; i++) {
      rows.add(new Row().setInt("id", i));
    }
    table.insert(rows);

    String plan =
        ((SqlDatabase) database)
            .getJooq()
            .fetch(
                "EXPLAIN SELECT operation, stamp, userid, tablename FROM \"TestChangelogIndex\".mg_changelog ORDER BY stamp DESC LIMIT 1")
            .getValues(0, String.class)
            .toString();
    assertTrue(plan.contains("Index Scan"), plan);
    assertTrue(plan.contains(ChangeLogExecutor.STAMP_INDEX), plan);
  }

  @Test
  void updateChangingOnlyUpdatedOnAddsNoChangelogRow() {
    Schema schema = setupChangelogSchema("TestChangelogUpdatedOnOnly");
    Table table = schema.create(table("Item", column("id").setType(INT).setPkey(), column("name")));
    table.insert(new Row().setInt("id", 1).setString("name", "a"));
    int before = schema.getChangesCount();

    table.update(
        table
            .retrieveRows()
            .get(0)
            .set(Constants.MG_UPDATEDON, java.time.LocalDateTime.of(2001, 1, 1, 10, 0)));
    assertEquals(before, schema.getChangesCount());

    table.update(
        table
            .retrieveRows()
            .get(0)
            .setString("name", "b")
            .set(Constants.MG_UPDATEDON, java.time.LocalDateTime.of(2002, 2, 2, 10, 0)));
    assertEquals(before + 1, schema.getChangesCount());
  }

  @Test
  void identicalSaveAndUpdateAddNoChangelogRowOnPlainAndSubclassTable() {
    Schema schema = setupChangelogSchema("TestChangelogIdenticalWrite");
    Table plain =
        schema.create(table("Plain", column("id").setType(INT).setPkey(), column("name")));
    schema.create(table("Root", column("id").setType(INT).setPkey(), column("name")));
    Table sub = schema.create(table("Sub", column("extra")).setInheritName("Root"));
    Row plainRow = new Row().setInt("id", 1).setString("name", "a");
    Row subRow = new Row().setInt("id", 1).setString("name", "a").setString("extra", "x");
    plain.save(plainRow);
    sub.save(subRow);
    int before = schema.getChangesCount();

    plain.save(plainRow);
    plain.update(plainRow);
    sub.save(subRow);
    sub.update(subRow);
    assertEquals(before, schema.getChangesCount());

    plain.save(new Row().setInt("id", 1).setString("name", "b"));
    assertEquals(before + 1, schema.getChangesCount());
    sub.save(new Row().setInt("id", 1).setString("name", "b").setString("extra", "x"));
    assertEquals(before + 2, schema.getChangesCount());
    sub.save(new Row().setInt("id", 1).setString("name", "b").setString("extra", "y"));
    assertEquals(before + 3, schema.getChangesCount());
  }

  @Test
  void changelogHoldsNoFileContents() {
    Schema schema = setupChangelogSchema("TestChangelogFiles");
    Table table =
        schema.create(
            table(
                "Doc",
                column("id").setType(INT).setPkey(),
                column("photo").setType(FILE),
                column("notes_contents")));
    Row row =
        new Row()
            .setInt("id", 1)
            .setString("notes_contents", "keepme")
            .setBinary("photo", new BinaryFileWrapper("text/plain", "a.txt", "bytes".getBytes()));
    table.insert(row);
    table.update(row.setString("notes_contents", "keepme2"));

    table.getMetadata().add(column("scan").setType(FILE));
    table.update(
        row.setBinary("scan", new BinaryFileWrapper("text/plain", "b.txt", "other".getBytes())));

    List<Change> changes = schema.getChanges(100);
    assertEquals(3, changes.size());
    for (Change change : changes) {
      for (String json : new String[] {change.oldRowData(), change.newRowData()}) {
        if (json != null) {
          assertFalse(json.contains("\"photo_contents\""), json);
          assertFalse(json.contains("\"scan_contents\""), json);
          assertTrue(json.contains("\"photo_mimetype\""), json);
          assertTrue(json.contains("\"notes_contents\""), json);
        }
      }
    }
    assertTrue(changes.get(1).newRowData().contains("keepme2"));
  }

  @Test
  void editorWritesAreLoggedThroughSharedAuditFunction() {
    String editor = "TestChangelogEditor";
    Schema schema = setupChangelogSchema("TestChangelogEditorSchema");
    schema.create(table("Item", column("id").setType(INT).setPkey(), column("name")));
    if (!database.hasUser(editor)) database.addUser(editor);
    schema.addMember(editor, Privileges.EDITOR.toString());
    int before = schema.getChangesCount();

    try {
      database.setActiveUser(editor);
      Table item = database.getSchema("TestChangelogEditorSchema").getTable("Item");
      item.insert(new Row().setInt("id", 1).setString("name", "a"));
      item.update(new Row().setInt("id", 1).setString("name", "b"));
    } finally {
      database.becomeAdmin();
    }

    assertEquals(before + 2, schema.getChangesCount());
  }

  @Test
  void changelogLogsAgainAfterDisableAndEnable() {
    Schema schema = setupChangelogSchema("TestChangelogToggle");
    Table table = schema.create(table("Item", column("id").setType(INT).setPkey()));
    schema.getMetadata().setSetting(Constants.IS_CHANGELOG_ENABLED, Boolean.FALSE.toString());
    schema.getMetadata().setSetting(Constants.IS_CHANGELOG_ENABLED, Boolean.TRUE.toString());
    int before = schema.getChangesCount();

    table.insert(new Row().setInt("id", 1));

    assertEquals(before + 1, schema.getChangesCount());
  }

  private Schema setupChangelogSchema(String schemaName) {
    database.dropSchemaIfExists(schemaName);
    database.becomeAdmin();
    database.createSchema(schemaName, "my desc");
    Schema schema = database.getSchema(schemaName);
    schema.getMetadata().setSetting(Constants.IS_CHANGELOG_ENABLED, Boolean.TRUE.toString());
    return schema;
  }

  private Schema setupSchemaWithTestDataAndChangelog(Database database, String schemaName) {
    database.dropSchemaIfExists(schemaName);
    database.becomeAdmin();
    database.createSchema(schemaName, "my desc");
    Schema schema = database.getSchema(schemaName);
    schema.getMetadata().setSetting(Constants.IS_CHANGELOG_ENABLED, Boolean.TRUE.toString());
    return addTestData(schema);
  }

  private Schema addTestData(Schema s) {
    Table person =
        s.create(
            table("Person")
                .add(column("ID").setType(INT).setPkey())
                .add(column("First_Name").setKey(2).setRequired(true))
                .add(column("Last_Name").setKey(2).setRequired(true)));

    Row kwik =
        new Row().setInt("ID", 3).setString("First_Name", "Kwik").setString("Last_Name", "Duck");
    Row kwek =
        new Row().setInt("ID", 4).setString("First_Name", "Kwek").setString("Last_Name", "Duck");
    Row kwak =
        new Row().setInt("ID", 5).setString("First_Name", "Kwak").setString("Last_Name", "Duck");
    person.insert(kwik, kwek, kwak);
    return s;
  }
}
