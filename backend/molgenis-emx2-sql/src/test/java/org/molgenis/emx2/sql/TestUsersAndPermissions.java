package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestUsersAndPermissions {
  public static final String ADMIN = "admin";
  public static final String ANONYMOUS = "anonymous";
  public static final String USER = "user";
  static Database database;
  private static final String TEST_ENABLE_USERS = "TestEnableUser";
  private static final String TEST_INITIALLY_ENABLE_USERS = "TestInitiallyEnableUser";
  private static final String TEST_NONEXISTENT_USERS = "TestNonExistentUser";
  private static final String TEST_ADMIN_USER = "TestAdminUser";
  private static final String TEST_ADMIN_USER_2 = "TestAdminUser2";

  @BeforeAll
  public static void setup() {
    database = TestDatabaseFactory.getTestDatabase();
  }

  @AfterAll
  public static void removeUsers() {
    if (database.hasUser(TEST_ENABLE_USERS)) {
      database.removeUser(TEST_ENABLE_USERS);
    }
    if (database.hasUser(TEST_ADMIN_USER)) {
      database.removeUser(TEST_ADMIN_USER);
    }
  }

  @Test
  void getUsers() {
    List<User> users = database.getUsers(1000, 0);
    assertTrue(users.size() > 0);

    int count = database.countUsers();
    assertEquals(count, users.size());
    users = database.getUsers(1000, 2);
    assertEquals(count - 2, users.size());
  }

  @Test
  void testActiveUser() {
    try {

      assertTrue(database.isAdmin());

      // add and set user
      String user1 = "Test Active User1";
      if (database.hasUser(user1)) {
        database.removeUser(user1);
      }
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
  void testDisableUser() {
    database.addUser(TEST_ENABLE_USERS);
    database.setEnabledUser(TEST_ENABLE_USERS, false);
    assertFalse(database.getUser(TEST_ENABLE_USERS).getEnabled());
  }

  @Test
  void testGrantAdminStatus() {
    database.addUser(TEST_ADMIN_USER);
    database.setAdminUser(TEST_ENABLE_USERS, true);
    assertTrue(database.getUser(TEST_ENABLE_USERS).isAdmin());
    database.setAdminUser(TEST_ENABLE_USERS, false);
    assertFalse(database.getUser(TEST_ENABLE_USERS).isAdmin());
  }

  @Test
  void testGrantAdminStatusAnonymous_shouldThrow() {
    assertThrows(MolgenisException.class, () -> database.setAdminUser(ANONYMOUS, true));
  }

  @Test
  void testRevokeAdminStatusOfAdmin_shouldThrow() {
    assertThrows(MolgenisException.class, () -> database.setAdminUser(ADMIN, false));
  }

  @Test
  void testGrantAdminByNonRootAdmin_shouldThrow() {
    database.becomeAdmin();
    database.addUser(TEST_ADMIN_USER);
    database.setAdminUser(TEST_ADMIN_USER, true);
    // Become non-root admin user
    database.setActiveUser(TEST_ADMIN_USER);
    database.addUser(TEST_ADMIN_USER_2);
    // Should throw only root admin can set admin
    assertThrows(MolgenisException.class, () -> database.setAdminUser(TEST_ADMIN_USER_2, true));
  }

  @Test
  void changeAdminPasswordByNonRootAdmin_shouldThrow() {
    database.addUser(TEST_ADMIN_USER);
    database.setAdminUser(TEST_ADMIN_USER, true);
    // Become non-root admin user
    database.setActiveUser(TEST_ADMIN_USER);
    // Should throw only root admin can set admin password
    assertThrows(MolgenisException.class, () -> database.setUserPassword(ADMIN, "somePassword"));
  }

  @Test
  void testNonExistentDisableUser() {
    try {
      database.setEnabledUser(TEST_NONEXISTENT_USERS, false);
      fail("should have failed");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  void testDisableAdmin() {
    try {
      database.setEnabledUser(ADMIN, false);
      fail("should have failed");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  void testDisableAnonymous() {
    try {
      database.setEnabledUser(ANONYMOUS, false);
      fail("should have failed");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  void testRemoveAdmin() {
    try {
      database.removeUser(ADMIN);
      fail("should have failed");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  void testRemoveAnonymous() {
    try {
      database.removeUser(ANONYMOUS);
      fail("should have failed");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  void testRemoveUser() {
    try {
      database.removeUser(USER);
      fail("should have failed");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  void testNonExistentRemoveUser() {
    try {
      database.removeUser(TEST_NONEXISTENT_USERS);
      fail("should have failed");
    } catch (Exception e) {
      // ok
    }
  }

  @Test
  void testInitiallyEnabledUser() {
    database.addUser(TEST_INITIALLY_ENABLE_USERS);
    assertTrue(database.getUser(TEST_INITIALLY_ENABLE_USERS).getEnabled());
  }

  @Test
  void testPassword() {
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
