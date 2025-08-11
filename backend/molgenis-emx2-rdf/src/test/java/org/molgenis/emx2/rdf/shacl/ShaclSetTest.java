package org.molgenis.emx2.rdf.shacl;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ShaclSetTest {
  @Test
  void testShaclSetEquality() {
    ShaclSet shacl1 =
        new ShaclSet(
            "my name", "a description", "1.0", new String[] {"a", "b"}, new String[] {"1", "2"});
    ShaclSet shacl2 =
        new ShaclSet(
            "my name", "a description", "1.0", new String[] {"a", "b"}, new String[] {"1", "2"});
    ShaclSet shacl3 =
        new ShaclSet("my name", "a description", "1.0", new String[] {"a"}, new String[] {"1"});

    assertEquals(shacl1, shacl2);
    assertNotEquals(shacl1, shacl3);
  }
}
