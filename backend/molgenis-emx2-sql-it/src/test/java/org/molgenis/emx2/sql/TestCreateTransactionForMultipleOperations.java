package org.molgenis.emx2.sql;

import java.sql.SQLException;
import org.junit.Assert;
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
          Table testTable =
              schema.create(TableMetadata.table("testCommit").add(Column.column("ColA").setPkey()));
          testTable.insert(new Row().setString("ColA", "test"));
          testTable.insert(new Row().setString("ColA", "DependencyOrderOutsideTransactionFails"));
        });
    db.clearCache();
    Assert.assertEquals(2, db.getSchema("testCommit").getTable("testCommit").retrieveRows().size());
  }

  @Test(expected = MolgenisException.class)
  public void testRollBack() {
    db.tx(
        db -> {
          Schema schema = db.dropCreateSchema("testRollBack");
          Table testTable =
              schema.create(
                  TableMetadata.table("testRollBack").add(Column.column("ColA").setKey(2)));
          Row r = new Row().setString("ColA", "test");
          testTable.insert(r);
          testTable.insert(r);
        });
  }
}
