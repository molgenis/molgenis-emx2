package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Constants.MG_EDIT_ROLE;
import static org.molgenis.emx2.TableMetadata.table;

import java.sql.SQLException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Privileges;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;

public class TestRowLevelSecurity {
  public static final String TEST_RLS_HAS_NO_PERMISSION = "test_rls_has_no_permission";
  public static final String TESTRLS_HAS_RLS_VIEW = "testrls_has_rls_view";
  public static final String TEST_RLS = "TestRLS";
  private static Database database;

  @BeforeAll
  public static void setUp() throws SQLException {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Disabled("because it tests unimplemented features")
  @Test
  public void testRls() {
    try {
      // create schema
      Schema s = database.dropCreateSchema(TEST_RLS);

      // create two users
      database.addUser(TEST_RLS_HAS_NO_PERMISSION);
      assertEquals(true, database.hasUser(TEST_RLS_HAS_NO_PERMISSION));

      database.addUser(TESTRLS_HAS_RLS_VIEW);

      // grant both owner on TestRLS schema so can add row level security
      s.addMember("testrls1", Privileges.OWNER.toString());
      s.addMember("testrls2", Privileges.OWNER.toString());

      s.addMember(
          TESTRLS_HAS_RLS_VIEW,
          Privileges.VIEWER.toString()); // can view table but only rows with right RLS

      // let one user create the table
      database.setActiveUser("testrls1");
      database.tx(
          db -> {
            db.getSchema(TEST_RLS).create(table(TEST_RLS).add(column("col1").setPkey()));
          });

      // let the other user add RLS
      database.setActiveUser("testrls2");
      database.tx(
          db -> {
            db.getSchema(TEST_RLS).getTable(TEST_RLS).getMetadata().enableRowLevelSecurity();
          });

      // let the first add a row (checks if admin permissions are setup correctly)
      database.setActiveUser("testrls1");
      database.tx(
          db -> {
            db.getSchema(TEST_RLS)
                .getTable(TEST_RLS)
                .insert(
                    new Row()
                        .setString("col1", "Hello World")
                        .set(MG_EDIT_ROLE, TESTRLS_HAS_RLS_VIEW),
                    new Row()
                        .setString("col1", "Hello World2")
                        .set(MG_EDIT_ROLE, TEST_RLS_HAS_NO_PERMISSION));
          });

      // let the second admin see it
      database.setActiveUser("testrls2");
      database.tx(
          db -> {
            assertEquals(2, db.getSchema(TEST_RLS).getTable(TEST_RLS).retrieveRows().size());
          });

      // have RLS user query and see one row
      database.setActiveUser(TESTRLS_HAS_RLS_VIEW);
      database.tx(
          db -> {
            assertEquals(1, db.getSchema(TEST_RLS).getTable(TEST_RLS).retrieveRows().size());
          });

      database.becomeAdmin();
      database.removeUser(TESTRLS_HAS_RLS_VIEW);
      assertEquals(false, database.hasUser(TESTRLS_HAS_RLS_VIEW));
    } finally {
      database.becomeAdmin();
    }
  }
}
