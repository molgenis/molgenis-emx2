package org.molgenis.emx2;

import org.jooq.JSONB;
import org.junit.Test;
import org.molgenis.emx2.utils.TypeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

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
    try {
      Object value = TypeUtils.getTypedValue("", type);
      fail("should have thrown exception, instead found: " + value);
    } catch (Exception e) {
      // aok
    }
  }
}
