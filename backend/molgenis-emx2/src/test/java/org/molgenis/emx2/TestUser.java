package org.molgenis.emx2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class TestUser {
  @Test
  public void testUser() {
    // silly test to get sonar green
    User u = new User("donald");
    User u2 = new User("donald");

    assertEquals(u, u2);

    u2.setUsername("mickey");

    assertNotEquals(u, u2);
  }
}
