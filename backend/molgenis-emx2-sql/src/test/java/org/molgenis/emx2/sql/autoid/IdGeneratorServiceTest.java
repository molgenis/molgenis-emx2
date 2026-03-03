package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jooq.Field;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class IdGeneratorServiceTest {

  private static final String SCHEMA_NAME = IdGeneratorServiceTest.class.getSimpleName();
  public static final String SNOWFLAKE_REGEX = "'[a-zA-Z\\d]{10}'";

  private static SqlDatabase database;
  private static IdGeneratorService service;
  private TableMetadata table;

  @BeforeAll
  static void setup() {
    database = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void setupSchema() {
    Schema schema = database.dropCreateSchema(SCHEMA_NAME);
    service = new IdGeneratorService();
    table = schema.create(TableMetadata.table("Person")).getMetadata();
  }

  @Test
  void givenNoneAutoIdColumn_thenThrow() {
    Column column = new Column("column").setType(ColumnType.BOOL);
    assertThrows(MolgenisException.class, () -> service.generateIdForColumn(column));
  }

  @Test
  void givenColumnWithNoComputed_thenUseSnowflakeFormat() {
    table.add(Column.column("column").setType(ColumnType.AUTO_ID));

    Column column = table.getColumn("column");
    assertColumnProducesIdMatchingFormat(column, SNOWFLAKE_REGEX);
  }

  @Test
  void givenAutoIdWithJustAutoIdToken_thenUseSnowflakeFormat() {
    table.add(
        Column.column("column")
            .setType(ColumnType.AUTO_ID)
            .setComputed(Constants.COMPUTED_AUTOID_TOKEN));

    Column column = table.getColumn("column");
    assertColumnProducesIdMatchingFormat(column, SNOWFLAKE_REGEX);
  }

  @Test
  void givenJustAutoIdWithPrePostFix_thenUseSnowflakeFormat() {
    table.add(
        Column.column("column")
            .setType(ColumnType.AUTO_ID)
            .setComputed("PRE-" + Constants.COMPUTED_AUTOID_TOKEN + "-POST"));

    Column column = table.getColumn("column");

    assertColumnProducesIdMatchingFormat(column, "'PRE-[a-zA-Z\\d]{10}-POST'");
  }

  @Test
  void givenColumnWithComputed_thenGenerateStrategy() {
    table.add(
        Column.column("column")
            .setType(ColumnType.AUTO_ID)
            .setComputed("${mg_autoid(length=2, format=numbers)}"));
    Column column = table.getColumn("column");
    Field<String> id = service.generateIdForColumn(column);
    assertEquals(
        id.toString(),
        """
              (
                select "MOLGENIS".mg_generate_autoid('IdGeneratorServiceTest', 'Person', 'column', '0123456789', 2)
              )""");
  }

  @Test
  void givenColumnWithMultipleAutoId_thenThrow() {
    table.add(
        Column.column("column")
            .setType(ColumnType.AUTO_ID)
            .setComputed("${mg_autoid(length=2, format=numbers)}${mg_autoid}"));
    Column column = table.getColumn("column");
    assertThrows(
        MolgenisException.class,
        () -> service.generateIdForColumn(column),
        "Cannot generate autoid for column ${mg_autoid(length=2, format=numbers)}${mg_autoid} because mg_autoid can only be used once");
  }

  private static void assertColumnProducesIdMatchingFormat(Column column, String regex) {
    Field<String> id = service.generateIdForColumn(column);
    Matcher matcher = Pattern.compile(regex).matcher(id.toString());
    assertTrue(
        matcher.find(),
        "Generated id: "
            + id
            + ", does not match expected pattern: "
            + matcher.pattern().toString());
  }
}
