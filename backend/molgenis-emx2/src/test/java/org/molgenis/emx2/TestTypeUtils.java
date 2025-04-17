package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.utils.TypeUtils.convertToCamelCase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.utils.TypeUtils;

public class TestTypeUtils {

  @Test
  public void test() {
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
  public void testCommaInCsvString() {
    String test = "\"value with, comma\",\"and, another\"";

    String[] result = TypeUtils.toStringArray(test);
    assertEquals("value with, comma", result[0]);
    assertEquals("and, another", result[1]);
  }

  @Test
  public void testDataTimeStringToDateTimeObject() {
    assertEquals(
        TypeUtils.toDateTime("2023-02-24T12:08:23.46378"),
        LocalDateTime.of(2023, 02, 24, 12, 8, 23, 463780000));
  }

  @Test
  void testToJsonb() throws JsonProcessingException {
    // object
    String objectString = "{\"key\":\"value\"}";
    JsonNode objectJackson = new ObjectMapper().readTree(objectString);
    JSONB objectJooq = JSONB.valueOf(objectString);

    // array
    String arrayString = "[\"string1\",\"string2\"]";
    JsonNode arrayJackson = new ObjectMapper().readTree(objectString);
    JSONB arrayJooq = JSONB.valueOf(objectString);

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
        () -> assertEquals(objectJooq, TypeUtils.toJsonb(objectString)),
        () -> assertEquals(objectJooq, TypeUtils.toJsonb(objectJackson)),
        () -> assertEquals(objectJooq, TypeUtils.toJsonb(objectJooq)),
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
  void testCamelCase() {
    assertAll(
        () -> assertEquals("aName", convertToCamelCase("a name")),
        () -> assertEquals("aNaMe", convertToCamelCase("a naMe")),
        () -> assertEquals("aNaMe", convertToCamelCase("A NaMe")),
        () -> assertEquals("aNaMe", convertToCamelCase("a na me")));
  }
}
