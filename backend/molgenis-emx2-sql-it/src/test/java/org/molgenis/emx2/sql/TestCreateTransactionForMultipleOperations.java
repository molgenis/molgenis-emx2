package org.molgenis.emx2.sql;

import static org.junit.Assert.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.SQLException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.*;

public class TestCreateTransactionForMultipleOperations {
  private static Database db;

  @BeforeClass
  public static void setUp() throws SQLException {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testCommit() {

    db.tx(
        db -> {
          Schema schema = db.dropCreateSchema("testCommit");
          Table testTable = schema.create(table("testCommit").add(column("ColA").setPkey()));
          testTable.insert(new Row().setString("ColA", "test"));
          testTable.insert(new Row().setString("ColA", "DependencyOrderOutsideTransactionFails"));
        });
    db.clearCache();
    assertEquals(2, db.getSchema("testCommit").getTable("testCommit").retrieveRows().size());
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack() {
    db.tx(
        db -> {
          Schema schema = db.dropCreateSchema("testRollBack");
          Table testTable = schema.create(table("testRollBack").add(column("ColA").setKey(2)));
          Row r = new Row().setString("ColA", "test");
          testTable.insert(r);
          testTable.insert(r);
        });
  }
}
