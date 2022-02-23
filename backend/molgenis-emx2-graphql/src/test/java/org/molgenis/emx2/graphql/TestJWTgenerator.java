package org.molgenis.emx2.graphql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.molgenis.emx2.JWTgenerator;

public class TestJWTgenerator {
  @Test
  public void testJWTgenerator() {
    JWTgenerator gen = new JWTgenerator();
    String result = gen.createTokenForUser("bofke", 60);
    System.out.println(result);
    assertNotEquals(result, "bofke");
    String user = gen.getUserFromToken(result);
    assertEquals(user, "bofke");
  }
}
