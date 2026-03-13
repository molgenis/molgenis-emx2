package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

public class PrimaryKeyTest {

  @Test
  void testSingleKeyEncodeAndDecode() {
    PrimaryKey pk = new PrimaryKey(Map.of("id", "Spike"));
    String encoded = pk.getEncodedString();
    assertEquals("id=Spike", encoded);
  }

  @Test
  void testCompositeKeyEncodeAndDecode() {
    PrimaryKey pk = new PrimaryKey(Map.of("firstName", "Donald", "lastName", "Duck"));
    String encoded = pk.getEncodedString();
    assertEquals("firstName=Donald&lastName=Duck", encoded);
  }

  @Test
  void testSpecialCharactersEncode() {
    PrimaryKey pk = new PrimaryKey(Map.of("name", "Donald Duck"));
    String encoded = pk.getEncodedString();
    assertEquals("name=Donald%20Duck", encoded);
  }

  @Test
  void testAmpersandInValueEncode() {
    PrimaryKey pk = new PrimaryKey(Map.of("title", "Tom & Jerry"));
    String encoded = pk.getEncodedString();
    assertEquals("title=Tom%20%26%20Jerry", encoded);
  }

  @Test
  void testEqualsSignInValueEncode() {
    PrimaryKey pk = new PrimaryKey(Map.of("formula", "x=y"));
    String encoded = pk.getEncodedString();
    assertEquals("formula=x%3Dy", encoded);
  }

  @Test
  void testNullValueThrowsException() {
    Map<String, String> mapWithNull = new TreeMap<>();
    mapWithNull.put("id", null);
    mapWithNull.put("name", "test");
    assertThrows(IllegalArgumentException.class, () -> new PrimaryKey(mapWithNull));
  }

  @Test
  void testEmptyKeysThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> new PrimaryKey(Map.of()));
  }
}
