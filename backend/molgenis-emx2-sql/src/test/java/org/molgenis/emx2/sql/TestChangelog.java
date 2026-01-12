package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.TableMetadata.table;

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
          Schema schema = setupSchemaWithTestDataAndChangelog(db, "testSchemaChangesWithOffset");
          List<Change> changes = schema.getChanges(1);
          assertEquals(1, changes.size());
          assertEquals("Person", changes.get(0).tableName());
          assertEquals('I', changes.get(0).operation());
          assertTrue(changes.get(0).newRowData().contains("Kwik"));
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
          assertTrue(changes.get(0).newRowData().contains("Kwek"));
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
