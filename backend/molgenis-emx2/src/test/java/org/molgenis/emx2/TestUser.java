package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

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
