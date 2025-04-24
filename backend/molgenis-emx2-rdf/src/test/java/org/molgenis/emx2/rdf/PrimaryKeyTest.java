package org.molgenis.emx2.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Operator.EQUALS;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.Filter;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.Table;
import org.molgenis.emx2.sql.TestDatabaseFactory;

public class PrimaryKeyTest {
  static Database database;
  static Schema primaryKeyTest;

  static final String ENCODED_KEY_BASIC = "complexPair=me%2C%20myself%20%26%20I&last=value";
  static final String ENCODED_KEY_BASIC_WRONG_ORDER =
      "last=value&complexPair=me%2C%20myself%20%26%20I";
  static final String ENCODED_COMPOSITE =
      "ref.firstId=myId1&ref.secondId.idAa=myId2&ref.secondId.idBB=myId3";

  static final SortedMap<String, String> basicKeys = new TreeMap<>();
  static final SortedMap<String, String> compositeKeys = new TreeMap<>();
  static final SortedMap<String, String> compositeKeys2 = new TreeMap<>();
  static final SortedMap<String, String> compositeKeys3 = new TreeMap<>();
  static final Map<String, String> basicKeysWrongOrder = new LinkedHashMap<>();

  static {
    basicKeys.put("last", "value");
    basicKeys.put("complex pair", "me, myself & I");

    compositeKeys.put("ref.first id", "myId1");
    compositeKeys.put("ref.second id.id Aa", "myId2");
    compositeKeys.put("ref.second id.id bB", "myId3");

    compositeKeys2.put("first id", "myId1");
    compositeKeys2.put("second id.id Aa", "myId2");
    compositeKeys2.put("second id.id bB", "myId3");

    compositeKeys3.put("id Aa", "myId2");
    compositeKeys3.put("id bB", "myId3");

    basicKeysWrongOrder.put("last", "value");
    basicKeysWrongOrder.put("complex pair", "me, myself & I");
  }

  @BeforeAll
  static void beforeAll() {
    database = TestDatabaseFactory.getTestDatabase();

    primaryKeyTest =
        database.dropCreateSchema(PrimaryKeyTest.class.getSimpleName() + "_primaryKeyTest");
    primaryKeyTest.create(
        table(
            "basic",
            column("last").setType(ColumnType.STRING).setPkey(),
            column("complex pair").setType(ColumnType.STRING).setPkey()),
        table(
            "composite", column("ref").setType(ColumnType.REF).setRefTable("composite2").setPkey()),
        table(
            "composite2",
            column("first id").setType(ColumnType.STRING).setPkey(),
            column("second id").setType(ColumnType.REF).setRefTable("composite3").setPkey()),
        table(
            "composite3",
            column("id Aa").setType(ColumnType.STRING).setPkey(),
            column("id bB").setType(ColumnType.STRING).setPkey()));

    primaryKeyTest.getTable("basic").insert(new Row(basicKeys));
    primaryKeyTest.getTable("composite3").insert(new Row(compositeKeys3));
    primaryKeyTest.getTable("composite2").insert(new Row(compositeKeys2));
    primaryKeyTest.getTable("composite").insert(new Row(compositeKeys));
  }

  @AfterAll
  static void afterAll() {
    database.dropSchemaIfExists(primaryKeyTest.getName());
  }

  @Test
  void testConstructorWrongOrderedLinkedHashMap() {
    // Calls generic Map constructor instead of SortedMap constructor so items get sorted.
    assertEquals(ENCODED_KEY_BASIC, new PrimaryKey(basicKeysWrongOrder).getEncodedString());
  }

  @Test
  void testEmptyConstructor() {
    assertThrows(IllegalArgumentException.class, () -> new PrimaryKey(Map.of()));
  }

  @Test
  void testConstructorWithNullValueInMap() {
    SortedMap<String, String> map = new TreeMap<>();
    map.put("key", null);
    assertThrows(IllegalArgumentException.class, () -> new PrimaryKey(map));
  }

  @Test
  void testFromRowBasic() {
    Table table = primaryKeyTest.getTable("basic");
    assertEquals(
        ENCODED_KEY_BASIC,
        PrimaryKey.fromRow(table, table.retrieveRows().get(0)).getEncodedString());
  }

  @Test
  void testFromRowComposite() {
    Table table = primaryKeyTest.getTable("composite");
    assertEquals(
        ENCODED_COMPOSITE,
        PrimaryKey.fromRow(table, table.retrieveRows().get(0)).getEncodedString());
  }

  @Test
  void testFromRowWrongTable() {
    Table table1 = primaryKeyTest.getTable("basic");
    Table table2 = primaryKeyTest.getTable("composite");

    assertThrows(
        IllegalArgumentException.class,
        () -> PrimaryKey.fromRow(table1, table2.retrieveRows().get(0)));
  }

  @Test
  void testFromEncodedStringBasic() {
    Table table = primaryKeyTest.getTable("basic");
    PrimaryKey primaryKey = PrimaryKey.fromEncodedString(table, ENCODED_KEY_BASIC);

    assertEquals(basicKeys.entrySet(), primaryKey.getKeys().entrySet());
  }

  @Test
  void testFromEncodedStringEmpty() {
    Table table = primaryKeyTest.getTable("basic");
    assertThrows(IllegalArgumentException.class, () -> PrimaryKey.fromEncodedString(table, ""));
  }

  @Test
  void testFromEncodedStringBasicWrongOrder() {
    Table table = primaryKeyTest.getTable("basic");
    assertThrows(
        IllegalArgumentException.class,
        () -> PrimaryKey.fromEncodedString(table, ENCODED_KEY_BASIC_WRONG_ORDER));
  }

  @Test
  void testFromEncodedStringComposite() {
    Table table = primaryKeyTest.getTable("composite");
    PrimaryKey primaryKey = PrimaryKey.fromEncodedString(table, ENCODED_COMPOSITE);

    assertEquals(compositeKeys.entrySet(), primaryKey.getKeys().entrySet());
  }

  /** Table does not contain correct columns for retrieving names from identifiers */
  @Test
  void testFromEncodedStringWrongTable() {
    Table table = primaryKeyTest.getTable("basic");
    assertThrows(
        IllegalArgumentException.class,
        () -> PrimaryKey.fromEncodedString(table, ENCODED_COMPOSITE));
  }

  @Test
  void testThatKeyCanBeConvertedToAFilter() {
    Table table = primaryKeyTest.getTable("basic");
    PrimaryKey key = PrimaryKey.fromEncodedString(table, ENCODED_KEY_BASIC);
    Filter filters = key.getFilter();
    assertNotNull(filters, "The filter should not be null.");
    assertEquals(
        2,
        filters.getSubfilters().size(),
        "The filter should contain filters for both conditions.");

    boolean filterComplex = false;
    boolean filterLast = false;
    for (var filter : filters.getSubfilters()) {
      if (filter.getColumn().equals("complex pair")
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains("me, myself & I")
          && filter.getValues().length == 1) {
        filterComplex = true;
      }
      if (filter.getColumn().equals("last")
          && filter.getOperator() == EQUALS
          && Arrays.stream(filter.getValues()).toList().contains("value")
          && filter.getValues().length == 1) {
        filterLast = true;
      }
    }
    assertTrue(filterComplex, "The filter should contain a sub filter for the complex key.");
    assertTrue(filterLast, "The filter should contain a sub filter for the last key.");
  }
}
