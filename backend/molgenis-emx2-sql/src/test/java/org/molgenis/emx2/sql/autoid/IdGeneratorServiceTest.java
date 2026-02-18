package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;
import org.molgenis.emx2.utils.generator.SnowflakeIdGenerator;

class IdGeneratorServiceTest {

  private static final String SCHEMA_NAME = IdGeneratorServiceTest.class.getSimpleName();

  private static SqlDatabase database;
  private static DSLContext jooq;

  private static final IdGeneratorService SERVICE = new IdGeneratorService(jooq);

  @BeforeAll
  static void setup() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setupSchema() {
    database.dropCreateSchema(SCHEMA_NAME);
    jooq = database.getJooq();
  }

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
