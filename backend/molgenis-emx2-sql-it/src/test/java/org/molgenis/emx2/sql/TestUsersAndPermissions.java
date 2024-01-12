package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestUsersAndPermissions {
  static Database database;

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void getUsers() {

    List<User> users = database.getUsers(1000, 0);
    assertTrue(users.size() > 0);

    int count = database.countUsers();
    assertEquals(count, users.size());

    users = database.getUsers(1000, 2);
    assertEquals(count - 2, users.size());
  }

  @Test
  public void testActiveUser() {
    try {

      assertTrue(database.isAdmin());

      // add and set user
      String user1 = "Test Active User1";
      if (database.hasUser(user1)) database.removeUser(user1);
      database.addUser(user1);
      database.setActiveUser(user1);
      assertEquals(user1, database.getActiveUser());

      // remove active user
      database.becomeAdmin();
      assertTrue(database.isAdmin());

      // create schema
      Schema schema1 = database.dropCreateSchema("TestActiveUser1");

      // create table without permission should fail
      database.setActiveUser(user1);

      try {
        schema1.create(table("Test"));
        fail("should have failed");
      } catch (MolgenisException e) {
        System.out.println("Failed correctly on create schema:\n" + e.toString());
      }

      // retry with proper permission
      database.becomeAdmin(); // god mode so I can edit membership
      schema1.addMember(user1, Privileges.MANAGER.toString());
      database.setActiveUser(user1);
      try {
        schema1.create(table("Test"));
      } catch (MolgenisException e) {
        fail("should be permitted");
      }

    } finally {
      database.becomeAdmin();
    }
  }

  @Test
  public void testPassword() {
    try {
      database.addUser("donald");
      database.setUserPassword("donald", "blaat");
      assertTrue(database.checkUserPassword("donald", "blaat"));
      assertFalse(database.checkUserPassword("donald", "blaat2"));

      // check if user can change their own password
      database.setActiveUser("donald");
      assertTrue(database.checkUserPassword("donald", "blaat"));
      assertFalse(database.checkUserPassword("donald", "blaat2"));

      // ensure otherwise fails
      database.becomeAdmin();
      database.addUser("katrien");
      database.setActiveUser("katrien");
      try {
        database.setUserPassword("donald", "blaat");
        fail("should have failed");
      } catch (Exception e) {
        // ok
      }
    } finally {
      database.becomeAdmin();
      database.removeUser("donald");
      database.removeUser("katrien");
    }
  }
}
