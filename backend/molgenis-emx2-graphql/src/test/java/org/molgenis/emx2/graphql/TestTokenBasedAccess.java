package org.molgenis.emx2.graphql;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.sql.JWTgenerator;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class TestTokenBasedAccess {
  Database db = TestDatabaseFactory.getTestDatabase();

  @Test
  public void testJWTgenerator() {
    db.addUser("tokentest1");
    db.addUser("tokentest2");

    // user environment variable

    // temp token
    JWTgenerator gen = new JWTgenerator();
    String result = gen.createTemporaryToken(db, "tokentest1");
    System.out.println(result);
    assertNotEquals(result, "tokentest1");
    String user = gen.getUserFromToken(db, result);
    assertEquals(user, "tokentest1");

    // named token
    result = gen.createNamedTokenForUser(db, "tokentest2", "mytoken");
    user = gen.getUserFromToken(db, result);
    assertEquals(user, "tokentest2");

    // check cannot make token for user if not authorized
    db.setActiveUser("tokentest1");
    try {
      result = gen.createNamedTokenForUser(db, "tokentest2", "mytoken");
      fail("should not be able to make token for other user, unless admin");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Cannot"));
      // fail correct
    }
  }

  // see webapi smoke tests for integrated token test
}
