package org.molgenis.emx2.sql;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.ColumnTypeGroups.*;

import java.util.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

class TestSqlTypeUtils {

  @BeforeAll
  static void before() {
    if (!SnowflakeIdGenerator.hasInstance()) {
      SnowflakeIdGenerator.init("123");
    }
  }

  @Test
  void testAllColumnTypesCoveredGetTypedValue() {
    Column column = mock(Column.class);
    Row row = mock(Row.class);

    EXCLUDE_REFERENCE_HEADING.forEach(
        columnType -> {
          when(column.getColumnType()).thenReturn(columnType);
          when(column.getPrimitiveColumnType()).thenReturn(columnType.getBaseType());
          SqlTypeUtils.getTypedValue(column, row);
        });
  }

  @Test
  void testAllColumnTypesCoveredGetPsqlType() {
    EXCLUDE_FILE_PERIOD_REFERENCE_HEADING.forEach(SqlTypeUtils::getPsqlType);
  }
}
