package org.molgenis.emx2.utils.generator;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;

class IdGeneratorServiceTest {

  private static final IdGeneratorService SERVICE = new IdGeneratorService();

  @Test
  void givenNoneAutoIdColumn_thenThrow() {
    Column column = new Column("column").setType(ColumnType.BOOL);
    assertThrows(MolgenisException.class, () -> SERVICE.generateIdForColumn(column));
  }

  @Test
  void givenColumnWithNoComputed_thenUseSnowflakeFormat() {
    SnowflakeIdGenerator.init("123");
    Column column = new Column("column").setType(ColumnType.AUTO_ID);
    String id = SERVICE.generateIdForColumn(column);
    Matcher matcher = Pattern.compile("[a-zA-Z\\d]{10}").matcher(id);
    assertTrue(
        matcher.find(),
        "Generated id: "
            + id
            + ", does not match expected pattern: "
            + matcher.pattern().toString());
  }

  @Test
  void givenColumnWithComputed_thenGenerateStrategy() {
    Column column =
        new Column("column")
            .setType(ColumnType.AUTO_ID)
            .setComputed("${mg_autoid(length=1, format=numbers)}");
    String id = SERVICE.generateIdForColumn(column);
    Matcher matcher = Pattern.compile("\\d").matcher(id);
    assertTrue(
        matcher.find(),
        "Generated id: "
            + id
            + ", does not match expected pattern: "
            + matcher.pattern().toString());
  }
}
