package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.Operator.EQUALS;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.Filter;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.TableMetadata;

public class PrimaryKeyTest {
  static final TableMetadata table = mock(TableMetadata.class);
  static final Row row = mock(Row.class);

  static final String FIRST_COLUMN_NAME = "another";
  static final String COMPLEX_COLUMN_NAME = "complex pair";
  static final String LAST_COLUMN_NAME = "last";

  static final String FIRST_COLUMN_ID = FIRST_COLUMN_NAME;
  static final String COMPLEX_COLUMN_ID = "complexPair";
  static final String LAST_COLUMN_ID = LAST_COLUMN_NAME;

  static final String FIRST_COLUMN_VALUE = "value1";
  static final String COMPLEX_COLUMN_VALUE = "me, myself & I";
  static final String LAST_COLUMN_VALUE = "value2";

  static final Column firstColumn = new Column(FIRST_COLUMN_NAME);
  static final Column complexColumn = new Column(COMPLEX_COLUMN_NAME);
  static final Column lastColumn = new Column(LAST_COLUMN_NAME);

  static final Map.Entry<String, String> KEY_FIRST =
      Map.entry(FIRST_COLUMN_NAME, FIRST_COLUMN_VALUE);
  static final Map.Entry<String, String> KEY_COMPLEX =
      Map.entry(COMPLEX_COLUMN_NAME, COMPLEX_COLUMN_VALUE);
  static final Map.Entry<String, String> KEY_LAST = Map.entry(LAST_COLUMN_NAME, LAST_COLUMN_VALUE);

  static final String ENCODED_KEY_FIRST = "another=value1";
  static final String ENCODED_KEY_FIRST_LAST = "another=value1&last=value2";
  static final String ENCODED_KEY_FIRST_LAST_WRONG_ORDER = "last=value2&another=value1";
  static final String ENCODED_KEY_FULL =
      "another=value1&complexPair=me%2C%20myself%20%26%20I&last=value2";

  static {
    when(table.getColumnByIdentifier(FIRST_COLUMN_ID)).thenReturn(firstColumn);
    when(table.getColumnByIdentifier(COMPLEX_COLUMN_ID)).thenReturn(complexColumn);
    when(table.getColumnByIdentifier(LAST_COLUMN_ID)).thenReturn(lastColumn);

    when(row.getString(firstColumn.getName())).thenReturn(FIRST_COLUMN_VALUE);
    when(row.getString(complexColumn.getName())).thenReturn(COMPLEX_COLUMN_VALUE);
    when(row.getString(lastColumn.getName())).thenReturn(LAST_COLUMN_VALUE);
  }

  @Test
  void testFromRowFirst() {
    when(table.getPrimaryKeyColumns()).thenReturn(List.of(firstColumn));
    assertEquals(ENCODED_KEY_FIRST, PrimaryKey.fromRow(table, row).getEncodedString());
  }

  @Test
  void testFromRowFirstLast() {
    // last column is given first (to validate that order does not matter)
    when(table.getPrimaryKeyColumns()).thenReturn(List.of(lastColumn, firstColumn));
    assertEquals(ENCODED_KEY_FIRST_LAST, PrimaryKey.fromRow(table, row).getEncodedString());
  }

  @Test
  void testFromRowFull() {
    // unsorted order (to validate that order does not matter)
    when(table.getPrimaryKeyColumns()).thenReturn(List.of(lastColumn, firstColumn, complexColumn));
    assertEquals(ENCODED_KEY_FULL, PrimaryKey.fromRow(table, row).getEncodedString());
  }

  @Test
  void testFromEncodedStringFirst() {
    SortedMap<String, String> keys =
        PrimaryKey.fromEncodedString(table, ENCODED_KEY_FIRST).getKeys();
    assertEquals(Map.ofEntries(KEY_FIRST).entrySet(), keys.entrySet());
  }

  @Test
  void testFromEncodedStringFirstLast() {
    SortedMap<String, String> keys =
        PrimaryKey.fromEncodedString(table, ENCODED_KEY_FIRST_LAST).getKeys();

    assertAll(
        () -> assertEquals(Map.ofEntries(KEY_FIRST, KEY_LAST).entrySet(), keys.entrySet()),
        () -> assertEquals(KEY_FIRST.getKey(), keys.firstKey()),
        () -> assertEquals(KEY_LAST.getKey(), keys.lastKey()));
  }

  @Test
  void testFromEncodedStringFirstLastWrongOrder() {
    assertThrows(
        IllegalArgumentException.class,
        () -> PrimaryKey.fromEncodedString(table, ENCODED_KEY_FIRST_LAST_WRONG_ORDER));
  }

  @Test
  void testFromEncodedStringFull() {
    SortedMap<String, String> keys =
        PrimaryKey.fromEncodedString(table, ENCODED_KEY_FULL).getKeys();

    assertAll(
        () ->
            assertEquals(
                Map.ofEntries(KEY_FIRST, KEY_COMPLEX, KEY_LAST).entrySet(), keys.entrySet()),
        () -> assertEquals(KEY_FIRST.getKey(), keys.firstKey()),
        // if entrySet equal, the one remaining is neither first nor last
        () -> assertEquals(KEY_LAST.getKey(), keys.lastKey()));
  }

  @Test
  void testFromEncodedStringEmpty() {
    assertThrows(IllegalArgumentException.class, () -> PrimaryKey.fromEncodedString(table, ""));
  }

  @Test
  void testThatKeyCanBeConvertedToAFilter() {
    PrimaryKey key = PrimaryKey.fromEncodedString(table, ENCODED_KEY_FULL);
    Filter filters = key.getFilter();
    assertNotNull(filters, "The filter should not be null.");
    assertEquals(
        3,
        filters.getSubfilters().size(),
        "The filter should contain filters for both conditions.");

    boolean filterFirst = false;
    boolean filterComplex = false;
    boolean filterLast = false;
    for (var filter : filters.getSubfilters()) {
      if (filter.getColumn().equals(KEY_FIRST.getKey())
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains(KEY_FIRST.getValue())
          && filter.getValues().length == 1) {
        filterFirst = true;
      }
      if (filter.getColumn().equals(KEY_COMPLEX.getKey())
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains(KEY_COMPLEX.getValue())
          && filter.getValues().length == 1) {
        filterComplex = true;
      }
      if (filter.getColumn().equals(KEY_LAST.getKey())
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains(KEY_LAST.getValue())
          && filter.getValues().length == 1) {
        filterLast = true;
      }
    }
    assertTrue(filterFirst, "The filter should contain a sub filter for the first key.");
    assertTrue(filterComplex, "The filter should contain a sub filter for the complex key.");
    assertTrue(filterLast, "The filter should contain a sub filter for the last key.");
  }
}
