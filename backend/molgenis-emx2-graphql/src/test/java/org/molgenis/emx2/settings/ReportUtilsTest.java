package org.molgenis.emx2.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class ReportUtilsTest {

  private static Schema schema;

  @BeforeAll
  static void setup() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(ReportUtilsTest.class.getSimpleName());
    schema.create(table("Pet", column("name").setPkey()));
    schema.getTable("Pet").insert(row("name", "a"), row("name", "b"), row("name", "c"));
    schema
        .getMetadata()
        .setSetting(
            "reports",
            """
            [
              {"id":"plain","sql":"select name from \\"Pet\\" order by name"},
              {"id":"semicolon","sql":"select name from \\"Pet\\" order by name; ;\\n "},
              {"id":"comment","sql":"select name from \\"Pet\\" order by name -- all pets"},
              {"id":"ownLimit","sql":"select name from \\"Pet\\" order by name limit 2"},
              {"id":"noSql"}
            ]
            """);
  }

  @Test
  void testTrailingSemicolonsAreIgnored() {
    assertEquals(
        "[{\"name\":\"a\"},{\"name\":\"b\"},{\"name\":\"c\"}]",
        ReportUtils.getReportAsJson("semicolon", schema, Map.of()));
    assertEquals(
        "[{\"name\":\"b\"}]", ReportUtils.getReportAsJson("semicolon", schema, Map.of(), 1, 1));
    assertEquals(3, ReportUtils.getReportCount("semicolon", schema, Map.of()));
    assertEquals(3, ReportUtils.getReportAsRows("semicolon", schema, Map.of()).size());
  }

  @Test
  void testTrailingCommentDoesNotHideLimit() {
    assertEquals(
        "[{\"name\":\"a\"},{\"name\":\"b\"}]",
        ReportUtils.getReportAsJson("comment", schema, Map.of(), 2, 0));
    assertEquals(3, ReportUtils.getReportCount("comment", schema, Map.of()));
  }

  @Test
  void testPagingAppliesWithinOwnLimit() {
    assertEquals(
        "[{\"name\":\"a\"},{\"name\":\"b\"}]",
        ReportUtils.getReportAsJson("ownLimit", schema, Map.of(), 10, 0));
    assertEquals(
        "[{\"name\":\"b\"}]", ReportUtils.getReportAsJson("ownLimit", schema, Map.of(), 10, 1));
    assertEquals(2, ReportUtils.getReportCount("ownLimit", schema, Map.of()));
  }

  @Test
  void testLimitAndOffsetApply() {
    assertEquals(
        "[{\"name\":\"b\"},{\"name\":\"c\"}]",
        ReportUtils.getReportAsJson("plain", schema, Map.of(), 2, 1));
  }

  @Test
  void testReportWithoutSqlGivesClearError() {
    MolgenisException exception =
        assertThrows(
            MolgenisException.class, () -> ReportUtils.getReportAsJson("noSql", schema, Map.of()));
    assertEquals("Report id=noSql has no sql", exception.getMessage());
  }
}
