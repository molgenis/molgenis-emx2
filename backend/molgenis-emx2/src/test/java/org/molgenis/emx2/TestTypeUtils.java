package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnTypeGroups.*;
import static org.molgenis.emx2.TableMetadata.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.utils.TypeUtils;

class TestTypeUtils {

  @Test
  void test() {
    executeTest(ColumnType.UUID_ARRAY, new UUID[] {UUID.randomUUID(), UUID.randomUUID()});
    executeTest(ColumnType.BOOL_ARRAY, new Boolean[] {true, false});
    executeTest(ColumnType.INT_ARRAY, new Integer[] {1, 2});
    executeTest(ColumnType.DECIMAL_ARRAY, new Double[] {1.0, 2.0});
    executeTest(ColumnType.DATE_ARRAY, new LocalDate[] {LocalDate.now(), LocalDate.now()});
    executeTest(
        ColumnType.DATETIME_ARRAY, new LocalDateTime[] {LocalDateTime.now(), LocalDateTime.now()});

    // test null string is trimmed to null correctly
    for (ColumnType type : ColumnType.values()) {
      // not applicable to file
      if (type.isAtomicType()) {
        assertNull(
            TypeUtils.getTypedValue("", type),
            "Empty string should result in null for columnType=" + type);
      }
    }

    // check that spaces are trimmed around string
    assertEquals("blaat", TypeUtils.toString(" blaat "));
  }

  private void executeTest(ColumnType type, Object[] b) {
    Object[] a = Arrays.copyOfRange(b, 0, 1);
    Object[] c = Arrays.copyOfRange(b, 0, 0);
    assertArrayEquals(a, (Object[]) TypeUtils.getTypedValue(a, type));
    assertArrayEquals(a, (Object[]) TypeUtils.getTypedValue(a[0].toString(), type));
    assertArrayEquals(b, (Object[]) TypeUtils.getTypedValue(b, type));
    assertArrayEquals(b, (Object[]) TypeUtils.getTypedValue(List.of(b), type));
    assertArrayEquals(
        b,
        (Object[]) TypeUtils.getTypedValue(new String[] {b[0].toString(), b[1].toString()}, type));
    assertArrayEquals(c, (Object[]) TypeUtils.getTypedValue(c, type));
    assertNull((Object[]) TypeUtils.getTypedValue(null, type));
    assertNull(TypeUtils.getTypedValue("", type));
  }

  @Test
  void testCommaInCsvString() {
    String test = "\"value with, comma\",\"and, another\"";

    String[] result = TypeUtils.toStringArray(test);
    assertEquals("value with, comma", result[0]);
    assertEquals("and, another", result[1]);
  }

  @Test
  void testDataTimeStringToDateTimeObject() {
    assertEquals(
        TypeUtils.toDateTime("2023-02-24T12:08:23.46378"),
        LocalDateTime.of(2023, 02, 24, 12, 8, 23, 463780000));
  }

  @Test
  void testToBool() {
    String falseString = "False";
    String trueString = "True";
    String zero = "0";
    String one = "1";
    String onePointZero = "1.0";
    String five = "5";
    String minusOne = "-1";
    assertNull(TypeUtils.toBool(null));
    assertEquals(false, TypeUtils.toBool(falseString));
    assertEquals(true, TypeUtils.toBool(trueString));
    assertEquals(false, TypeUtils.toBool(zero));
    assertEquals(true, TypeUtils.toBool(one));
    assertThrows(MolgenisException.class, () -> TypeUtils.toBool(onePointZero));
    assertThrows(MolgenisException.class, () -> TypeUtils.toBool(five));
    assertThrows(MolgenisException.class, () -> TypeUtils.toBool(minusOne));
  }

  @Test
  void testToDecimal() {
    assertNull(TypeUtils.toDecimal(null));
    assertNull(TypeUtils.toDecimal(""));
    assertNull(TypeUtils.toDecimal("\n"));

    assertEquals(15, TypeUtils.toDecimal("15.0"));
    assertEquals(-15, TypeUtils.toDecimal("-15.0"));
    assertEquals(15, TypeUtils.toDecimal(new BigDecimal(15)));
    assertEquals(15, TypeUtils.toDecimal(15));
    assertEquals(15, (double) 15);
    assertEquals(15, TypeUtils.toDecimal(15L));
  }

  @Test
  void testToJsonb() throws JsonProcessingException {
    // object
    String objectString = "{\"key\":\"value\"}";
    JsonNode objectJackson = new ObjectMapper().readTree(objectString);
    JSONB objectJooq = JSONB.valueOf(objectString);

    // array
    String arrayString = "[\"string1\",\"string2\"]";
    JsonNode arrayJackson = new ObjectMapper().readTree(arrayString);
    JSONB arrayJooq = JSONB.valueOf(arrayString);

    // string
    String stringString = "\"string1\"";
    JsonNode stringJackson = new ObjectMapper().readTree(stringString);
    JSONB stringJooq = JSONB.valueOf(stringString);

    // null
    String nullString = "null";
    JsonNode nullJackson = new ObjectMapper().readTree(nullString);
    JSONB nullJooq = JSONB.valueOf(nullString);

    // invalid
    String nonUniqueKey = "{\"key1\":\"value1\", \"key1\":\"value2\"}";
    String externalComma = "{\"key1\":\"value1\"},{\"key2\":\"value2\"}";
    String trailingData = "{\"key\":\"value\"}trailing";
    int invalidJavaType = 1;

    assertAll(
        // valid: object
        () -> assertEquals(objectJooq, TypeUtils.toJsonb(objectString)),
        () -> assertEquals(objectJooq, TypeUtils.toJsonb(objectJackson)),
        () -> assertEquals(objectJooq, TypeUtils.toJsonb(objectJooq)),
        // valid: array
        () -> assertEquals(arrayJooq, TypeUtils.toJsonb(arrayString)),
        () -> assertEquals(arrayJooq, TypeUtils.toJsonb(arrayJackson)),
        () -> assertEquals(arrayJooq, TypeUtils.toJsonb(arrayJooq)),
        // invalid: primitive - string
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(stringString)),
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(stringJackson)),
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(stringJooq)),
        // invalid: primitive - null
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(nullString)),
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(nullJackson)),
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(nullJooq)),
        // invalid: non-unique key
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(nonUniqueKey)),
        // invalid: 2 objects separated by a comma (not in an array)
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(externalComma)),
        // invalid: trailing data
        () -> assertThrows(MolgenisException.class, () -> TypeUtils.toJsonb(trailingData)),
        // invalid: Java type int
        () -> assertThrows(ClassCastException.class, () -> TypeUtils.toJsonb(invalidJavaType)));
  }

  @Test
  void testAllColumnTypesCoveredGetArrayType() {
    EXCLUDE_ARRAY_FILE_REFERENCE_HEADING.forEach(TypeUtils::getArrayType);
  }

  @Test
  void testAllColumnTypesCoveredToJooqType() {
    EXCLUDE_REFERENCE_HEADING.forEach(TypeUtils::toJooqType);
  }

  @Test
  void givenNullRefArrayValue_whenConvertingToRow_thenSetEmptyList() {
    SchemaMetadata schema = new SchemaMetadata("test");
    schema.create(
        table("Tag", column("id").setType(ColumnType.STRING).setKey(1)),
        table("Resource", column("tags").setType(ColumnType.REF_ARRAY).setRefTable("Tag")));

    Column tagsColumn = schema.getTableMetadata("Resource").getColumn("tags");
    Row row = new Row();
    TypeUtils.addFieldObjectToRow(tagsColumn, null, row);

    assertTrue(row.containsName("tags"), "field should be present even when input list is null");
    assertEquals(null, row.getValueMap().get("tags"));
  }

  @Test
  void testAllColumnTypesCoveredTypedValue() {
    Object object = new Object();

    for (ColumnType columnType : EXCLUDE_FILE_REFERENCE_HEADING) {
      try {
        TypeUtils.getTypedValue(object, columnType);
      } catch (RuntimeException e) {
        if (e instanceof UnsupportedOperationException) {
          fail("ColumnType not covered: " + columnType);
        }
      }
    }
  }

  @Test
  void checkEnumMembershipNonEnumColumnIsNoOp() {
    Column col = column("status").setType(ColumnType.STRING);
    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(col, "anything"),
        "Non-enum column must not validate membership");
  }

  @Test
  void checkEnumMembershipEnumWithNoValuesAcceptsAnyString() {
    Column col = column("status").setType(ColumnType.ENUM);
    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(col, "anything"),
        "ENUM with no declared values must accept any string");

    Column colEmpty = column("status2").setType(ColumnType.ENUM).setValues(new String[0]);
    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(colEmpty, "anything"),
        "ENUM with empty values list must accept any string");
  }

  @Test
  void checkEnumMembershipScalarEnforcesAllowedSet() {
    Column col = column("priority").setType(ColumnType.ENUM).setValues("low", "medium", "high");

    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(col, "high"), "In-set value must be accepted");

    assertThrows(
        MolgenisException.class,
        () -> TypeUtils.checkEnumMembership(col, "critical"),
        "Out-of-set value must throw MolgenisException");
  }

  @Test
  void checkEnumMembershipArrayEnforcesAllowedSet() {
    Column enumArray =
        column("tags").setType(ColumnType.ENUM_ARRAY).setValues("alpha", "beta", "gamma");

    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(enumArray, new String[] {"alpha", "gamma"}),
        "All-in-set array must be accepted");

    assertThrows(
        MolgenisException.class,
        () -> TypeUtils.checkEnumMembership(enumArray, new String[] {"alpha", "unknown"}),
        "Array with one out-of-set element must throw MolgenisException");

    Column moduleArray = column("panels").setType(ColumnType.MODULE_ARRAY).setValues("A", "B");

    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(moduleArray, new String[] {"A"}),
        "MODULE_ARRAY bare in-set value must be accepted");

    assertThrows(
        MolgenisException.class,
        () -> TypeUtils.checkEnumMembership(moduleArray, new String[] {"A", "C"}),
        "MODULE_ARRAY with one out-of-set element must throw MolgenisException");
  }

  @Test
  void enumExactMatchEnforced() {
    Column enumCol = column("priority").setType(ColumnType.ENUM).setValues("low", "medium", "high");

    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(enumCol, "high"), "exact in-set value accepted");

    assertThrows(
        MolgenisException.class,
        () -> TypeUtils.checkEnumMembership(enumCol, "HIGH"),
        "ENUM must enforce exact strings");

    Column enumArrayCol = column("tags").setType(ColumnType.ENUM_ARRAY).setValues("alpha", "beta");

    assertThrows(
        MolgenisException.class,
        () -> TypeUtils.checkEnumMembership(enumArrayCol, new String[] {"Alpha"}),
        "ENUM_ARRAY must enforce exact strings");

    Column moduleArrayCol = column("panels").setType(ColumnType.MODULE_ARRAY).setValues("Mod1");

    assertThrows(
        MolgenisException.class,
        () -> TypeUtils.checkEnumMembership(moduleArrayCol, new String[] {"schema.Mod1"}),
        "MODULE_ARRAY must enforce exact strings; dotted value must not match bare declared value");
  }

  @Test
  void checkEnumMembershipNullValueIsNoOp() {
    Column col = column("priority").setType(ColumnType.ENUM).setValues("low", "high");
    assertDoesNotThrow(
        () -> TypeUtils.checkEnumMembership(col, null), "Null value must be a no-op");
  }
}
