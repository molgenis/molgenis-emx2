package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.TableMetadata.table;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;

public class TestChangelog {
  private static Database database;

  @BeforeClass
  public static void setUp() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testChangelog() {

    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          db.dropSchemaIfExists("testSchemaChanges");
          db.becomeAdmin();
          db.createSetting("CHANGELOG_SCHEMAS", "testSchemaChanges");
          db.createSchema("testSchemaChanges");
          Schema schema = db.getSchema("testSchemaChanges");
          schema = addTestData(schema);

          assertEquals(3, schema.getChanges(100).size());
          assertEquals('I', schema.getChanges(100).get(0).operation());
          assertEquals("Person", schema.getChanges(100).get(0).tableName());
        });
  }

  @Test
  public void testChangelogFeatureFlag() {

    database.tx(
        // prevent side effect of user changes on other tests using tx
        db -> {
          db.dropSchemaIfExists("testSchemaChangesFF");
          db.becomeAdmin();
          db.createSetting("CHANGELOG_SCHEMAS", "otherschema");
          db.createSchema("testSchemaChangesFF");
          Schema schema = db.getSchema("testSchemaChangesFF");
          schema = addTestData(schema);

          assertEquals(0, schema.getChanges(100).size());
        });
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
