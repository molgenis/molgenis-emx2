package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Operator.EQUALS;

import java.util.*;
import org.junit.jupiter.api.Test;

public class PrimaryKeyTest {

  static final Map.Entry<String, String> COMPLEX_KEY = Map.entry("complex pair", "me, myself & I");
  static final Map.Entry<String, String> LAST_KEY = Map.entry("last", "value");
  // Linked Map to enforce order
  static final Map<String, String> KEYS_MAP = new LinkedHashMap<>();
  static final String ENCODED_KEY = "complexPair=me%2C%20myself%20%26%20I&last=value";

  static {
    KEYS_MAP.put(LAST_KEY.getKey(), LAST_KEY.getValue());
    KEYS_MAP.put(COMPLEX_KEY.getKey(), COMPLEX_KEY.getValue());
  }

  @Test
  void testThatAPrimaryKeyIsSorted() {
    var key = new PrimaryKey(KEYS_MAP).getEncodedValue();
    assertEquals(ENCODED_KEY, key);
  }

  @Test
  void testThatAPrimaryKeyMustHaveAtLeastOneComponent() {
    assertThrows(IllegalArgumentException.class, () -> new PrimaryKey(Map.of()));
  }

  @Test
  void testThatKeyIsDecodedSuccesfully() {
    var key = PrimaryKey.makePrimaryKeyFromEncodedKey(ENCODED_KEY);
    assertNotNull(key, "Should be able to decode the key");
    assertEquals(2, key.getKeys().size(), "The key should contain two values");
    assertTrue(
        key.getKeys().entrySet().contains(COMPLEX_KEY),
        "The first key should have been decoded successfully");
    assertTrue(
        key.getKeys().entrySet().contains(LAST_KEY),
        "The last key should have been decoded successfully");
  }

  @Test
  void testThatKeyCanBeConvertedToAFilter() {
    var key = new PrimaryKey(KEYS_MAP);
    var filters = key.getFilter();
    assertNotNull(filters, "The filter should not be null.");
    assertEquals(
        2,
        filters.getSubfilters().size(),
        "The filter should contain filters for both conditions.");
    boolean filterFirst = false;
    boolean filterLast = false;
    for (var filter : filters.getSubfilters()) {
      if (filter.getColumn().equals(COMPLEX_KEY.getKey())
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains(COMPLEX_KEY.getValue())
          && filter.getValues().length == 1) {
        filterFirst = true;
      }
      if (filter.getColumn().equals(LAST_KEY.getKey())
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains(LAST_KEY.getValue())
          && filter.getValues().length == 1) {
        filterLast = true;
      }
    }
    assertTrue(filterFirst, "The filter should contain a sub filter for the first key.");
    assertTrue(filterLast, "The filter should contain a sub filter for the last key.");
  }

  @Test
  void testEncodedValuesForDifferentSizes() {
    assertAll(
        () -> assertEquals("a=1", new PrimaryKey(Map.of("a", "1")).getEncodedValue()),
        () -> assertEquals("a=1&b=2", new PrimaryKey(Map.of("a", "1", "b", "2")).getEncodedValue()),
        () ->
            assertEquals(
                "a=1&b=2&c=3",
                new PrimaryKey(Map.of("a", "1", "b", "2", "c", "3")).getEncodedValue()));
  }
}
