package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.INT;
import static org.molgenis.emx2.ColumnType.REF;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.StopWatch;

@Tag("slow")
public class TestBatchRequestsForSpeed {
  private static Database db;

  @BeforeAll
  public static void setUp() throws SQLException {
    // jdbc:postgresql://mswertz-test-psql1.postgres.database.azure.com:5432/{your_database}?user=molgenis@mswertz-test-psql1&password={your_password}&sslmode=require
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testBatch() {

    StopWatch.start("testBatch started");

    Schema schema = db.dropCreateSchema("testBatch");
    Table testBatchTable =
        schema.create(
            table("TestBatchRequestsForSpeed", column("test").setPkey(), column("testint", INT)));

    int size = 10000;
    StopWatch.print("Schema created");

    List<Row> rows = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Row r = new Row();
      r.setString("test", "test" + i);
      r.setInt("testint", i);
      rows.add(r);
    }
    StopWatch.print("Generated " + size + " test records", size);

    testBatchTable.insert(rows.subList(0, 100));

    StopWatch.print("Inserted first batch", 100);

    testBatchTable.insert(rows.subList(100, 200));

    StopWatch.print("Inserted second batch", 100);

    testBatchTable.insert(rows.subList(200, 1000));

    StopWatch.print("Inserted third batch", 800);

    testBatchTable.insert(rows.subList(1000, 10000));

    StopWatch.print("Inserted forth batch", 9000);

    rows = testBatchTable.retrieveRows();
    assertEquals(10000, rows.size());
    StopWatch.print("Queried all", 10000);

    for (Row r : rows) {
      r.setString("test", r.getString("test") + "_updated");
    }
    testBatchTable.update(rows);
    StopWatch.print("Batch update ", rows.size());

    StopWatch.print(
        "Retrieved", schema.getTable("TestBatchRequestsForSpeed").retrieveRows().size());
  }

  @Test
  public void testCreate() {

    StopWatch.start("");

    Schema schema = db.dropCreateSchema("testCreate");

    String PERSON = "Person";
    Table personTable =
        schema.create(
            table(
                PERSON,
                column("ID").setType(INT).setPkey(),
                column("First_Name").setRequired(true).setKey(2),
                column("Last_Name").setKey(2).setRequired(true),
                column("Father").setType(REF).setRefTable(PERSON)));

    for (int i = 0; i < 10; i++) {
      String name = PERSON + i;
      schema.create(
          table(
              name,
              column("ID").setType(INT).setPkey(),
              column("First_Name").setRequired(true).setKey(2),
              column("Last_Name").setKey(2).setRequired(true),
              column("Father").setType(REF).setRefTable(name)));
    }
    StopWatch.print("Created tables");

    // reinitialise database to see if it can recreate from background
    StopWatch.print("reloading database from disk");

    db.clearCache();
    schema = db.getSchema("testCreate");
    assertEquals(11, schema.getTableNames().size());
    StopWatch.print("reloading complete");

    // insert
    Table personTableReloaded = schema.getTable(PERSON);
    List<Row> rows = new ArrayList<>();
    int count = 1000;
    for (int i = 0; i < count; i++) {
      rows.add(
          new Row()
              .setInt("ID", i)
              .setString("Last_Name", "Duck" + i)
              .setString("First_Name", "Donald"));
    }
    System.out.println("Metadata" + personTableReloaded);
    personTableReloaded.insert(rows);

    StopWatch.print("insert", count);

    // queryOld
    Query q = schema.getTable(PERSON).query();
    StopWatch.print("QueryOld ", q.retrieveRows().size());

    // delete
    personTableReloaded.delete(rows);
    StopWatch.print("Delete", count);

    assertEquals(0, schema.getTable("Person").retrieveRows().size());
    assertEquals(2, personTableReloaded.getMetadata().getKeys().size());
    assertEquals(2, personTable.getMetadata().getKeys().size());
    personTable.getMetadata().removeKey(2);
    assertEquals(1, personTable.getMetadata().getKeys().size());
    assertEquals(9, personTable.getMetadata().getColumns().size());
    try {
      personTable.getMetadata().dropColumn("ID");
      fail("you shouldn't be allowed to remove primary key column");
    } catch (Exception e) {
      // good stuff
    }
    personTable.getMetadata().dropColumn("Father");
    assertEquals(8, personTable.getMetadata().getColumns().size());

    // drop a fromTable
    db.getSchema("testCreate").getMetadata().drop(personTable.getName());
    assertNull(db.getSchema("testCreate").getTable(personTable.getName()));

    // make sure nothing was left behind in backend
    db.clearCache();
    assertNull(db.getSchema("testCreate").getTable(personTable.getName()));
  }
}
