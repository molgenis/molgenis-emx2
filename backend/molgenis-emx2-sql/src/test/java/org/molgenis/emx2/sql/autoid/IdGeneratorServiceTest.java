package org.molgenis.emx2.sql.autoid;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.sql.SqlDatabase;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class IdGeneratorServiceTest {

  private static final String SCHEMA_NAME = IdGeneratorServiceTest.class.getSimpleName();

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
    DSLContext jooq = database.getJooq();
    service = new IdGeneratorService(jooq);
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
    String id = service.generateIdForColumn(column);
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
    table.add(
        Column.column("column")
            .setType(ColumnType.AUTO_ID)
            .setComputed("${mg_autoid(length=1, format=numbers)}"));
    Column column = table.getColumn("column");
    String id = service.generateIdForColumn(column);
    Matcher matcher = Pattern.compile("\\d").matcher(id);
    assertTrue(
        matcher.find(),
        "Generated id: "
            + id
            + ", does not match expected pattern: "
            + matcher.pattern().toString());
  }
}
