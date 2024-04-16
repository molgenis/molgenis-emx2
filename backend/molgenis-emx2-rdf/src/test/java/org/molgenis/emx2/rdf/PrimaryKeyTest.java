package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.EQUALS;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Filter;

public class PrimaryKeyTest {

  public static final Map.Entry<String, String> LAST_KEY = Map.entry("last", "value1");
  public static final Map.Entry<String, String> FIRST_KEY = Map.entry("first", "value2");
  public static final Map<String, String> KEY =
      Map.of(LAST_KEY.getKey(), LAST_KEY.getValue(), FIRST_KEY.getKey(), FIRST_KEY.getValue());
  public static final String ENCODED_KEY = "first=value2&last=value1";

  public static final Filter FIRST_KEY_FILTER = f("first", EQUALS, "value2");
  public static final Filter LAST_KEY_FILTER = f("last", EQUALS, "value1");

  @Test
  void testThatAPrimaryKeyIsSorted() {
    var key = new PrimaryKey(KEY).getEncodedValue();
    assertEquals(ENCODED_KEY, key);
  }

  @Test
  void testThatAPrimaryKeyMustHaveAtLeastOneComponent() {
    var pairs = new HashMap<String, String>();
    try {
      var key = new PrimaryKey(pairs);
      assertNull(key, "Should have thrown an exception during initialisation");
    } catch (Exception e) {
      // Expected
    }
  }

  @Test
  void testThatKeyIsDecodedSuccesfully() {
    var key = PrimaryKey.makePrimaryKeyFromEncodedKey(ENCODED_KEY);
    assertNotNull(key, "Should be able to decode the key");
    assertEquals(2, key.getKeys().size(), "The key should contain two values");
    assertTrue(
        key.getKeys().entrySet().contains(FIRST_KEY),
        "The first key should have been decoded successfully");
    assertTrue(
        key.getKeys().entrySet().contains(LAST_KEY),
        "The last key should have been decoded successfully");
  }

  @Test
  void testThatKeyCanBeConvertedToAFilter() {
    var pairs =
        Map.of(LAST_KEY.getKey(), LAST_KEY.getValue(), FIRST_KEY.getKey(), FIRST_KEY.getValue());
    var key = new PrimaryKey(pairs);
    var filters = key.getFilter();
    assertNotNull(filters, "The filter should not be null.");
    assertEquals(
        2,
        filters.getSubfilters().size(),
        "The filter should contain filters for both conditions.");
    boolean filterFirst = false;
    boolean filterLast = false;
    for (var filter : filters.getSubfilters()) {
      if (filter.getColumn().equals("first")
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains("value2")
          && filter.getValues().length == 1) {
        filterFirst = true;
      }
      if (filter.getColumn().equals("last")
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains("value1")
          && filter.getValues().length == 1) {
        filterLast = true;
      }
    }
    assertTrue(filterFirst, "The filter should contain a sub filter for the first key.");
    assertTrue(filterLast, "The filter should contain a sub filter for the last key.");
  }
}
