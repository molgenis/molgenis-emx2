package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.utils.TypeUtils.convertToTitleCase;

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
    executeTest(
        ColumnType.JSONB_ARRAY,
        new JSONB[] {JSONB.valueOf("{name:\"blaat\"}"), JSONB.valueOf("{name2:\"blaat2\"}")});

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
  void testConvertToTitleCase() {
    assertEquals("Table1", convertToTitleCase("table1"));
    assertEquals("Table_1", convertToTitleCase("table_1"));
    assertEquals("Table_1abc", convertToTitleCase("table_1abc"));
    assertEquals("Table a1abc", convertToTitleCase("tableA1abc"));
    assertEquals("Variable mappings", convertToTitleCase("variableMappings"));
    assertEquals("Variable mappings", convertToTitleCase("VariableMappings"));
    assertEquals("VCF tools", convertToTitleCase("VCFtools"));
    assertEquals("My VCF tools", convertToTitleCase("myVCFtools"));
    assertEquals("My VCF tools", convertToTitleCase("myVCFtools"));
  }
}
