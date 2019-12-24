package org.molgenis.emx2.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.DefaultRoles;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.MolgenisException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.molgenis.emx2.TableMetadata.table;

public class TestUsersAndPermissions {
  static Database database;

  @BeforeClass
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testActiveUser() {
    try {

      assertNull(database.getActiveUser());

      // add and set user
      String user1 = "Test Active User1";
      database.addUser(user1);
      database.setActiveUser(user1);
      assertEquals(user1, database.getActiveUser());

      // remove active user
      database.clearActiveUser();
      assertNull(database.getActiveUser());

      // create schema
      Schema schema1 = database.createSchema("TestActiveUser1");

      // create table without permission should fail
      database.setActiveUser(user1);
      try {
        schema1.create(table("Test"));
        fail("should have failed");
      } catch (MolgenisException e) {
        System.out.println("Failed correctly on create schema:\n" + e.toString());
      }

      // retry with proper permission
      database.clearActiveUser(); // god mode so I can edit membership
      schema1.addMember(user1, DefaultRoles.MANAGER.toString());
      database.setActiveUser(user1);
      try {
        schema1.create(table("Test"));
      } catch (MolgenisException e) {
        fail("should be permitted");
      }

    } finally {
      database.setActiveUser(null);
    }
  }
}
