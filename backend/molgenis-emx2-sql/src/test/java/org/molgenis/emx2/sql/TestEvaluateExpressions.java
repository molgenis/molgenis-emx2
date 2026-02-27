package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;
import static org.molgenis.emx2.sql.SqlTypeUtils.applyValidationAndComputed;
import static org.molgenis.emx2.sql.SqlTypeUtils.checkValidation;
import static org.molgenis.emx2.utils.JavaScriptUtils.executeJavascriptOnMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.molgenis.emx2.*;

class TestEvaluateExpressions {

  private static SqlDatabase db;
  private static Schema schema;

  @BeforeAll
  static void setUp() {
    db = (SqlDatabase) TestDatabaseFactory.getTestDatabase();
    schema = db.dropCreateSchema(TestEvaluateExpressions.class.getSimpleName());
  }

  @Test
  void testCheckValidationColumnsSuccess() {

    // should not throw exception
    executeJavascriptOnMap("columnName2", Map.of("columnName2", true));
    executeJavascriptOnMap("columnName2 > 1", Map.of("columnName2", 2));

    String error = checkValidation("columnName2 > 1", Map.of("columnName2", 1));
    assertEquals("columnName2 > 1", error);

    error = checkValidation("/^([a-z]+)$/.test(name)", Map.of("name", "123"));
    assertEquals("/^([a-z]+)$/.test(name)", error);

    error = checkValidation("/^([a-z]+)$/.test(name)", Map.of("name", "abc"));
    assertNull(error);

    error =
        checkValidation(
            "if(!/^([a-z]+)$/.test(name))'name should contain only lowercase letters'",
            Map.of("name", "123"));
    assertEquals("name should contain only lowercase letters", error);

    error =
        checkValidation(
            "if(!/^([a-z]+)$/.test(name))'name should contain only lowercase letters'",
            Map.of("name", "abc"));
    assertNull(error);
  }

  @Test
  void testCheckValidationColumnsFailure() {
    String expectedError = "script failed: ReferenceError: columnName2 is not defined";
    Map<String, Object> values = Map.of();
    assertThrows(
        MolgenisException.class, () -> checkValidation("columnName2", values), expectedError);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "false && true",
        "5 + 37",
        "new Date()",
      })
  void evaluateValidationExpressionTestSuccess(String expression) {
    assertNotNull(checkValidation(expression, Map.of()));
  }

  @Test
  void testCalculateComputedExpression() {
    String expression = "5 + 7";
    Object outcome = executeJavascriptOnMap(expression, Map.of());
    assertEquals(12, outcome);
  }

  @Test
  void testCalculateComputedExpressionFailure() {
    String expression = "5 + YAAARGH";
    try {
      checkValidation(expression, Map.of());
    } catch (MolgenisException exception) {
      assertTrue(exception.getMessage().contains("YAAARGH is not defined"));
    }
  }

  @Test
  void testCheckValidationSuccess() {
    String validation = "true && true";
    TableMetadata tableMetadata = table("Test", new Column("name").setValidation(validation));
    assertDoesNotThrow(
        () -> applyValidationAndComputed(tableMetadata.getColumns(), new Row(), db.getJooq()));
  }

  @Test
  void testCheckValidationWithCapitalColumnNameSuccess() {
    String validation = "name === 'pietje'";
    TableMetadata tableMetadata = table("Test", new Column("Name").setValidation(validation));
    assertDoesNotThrow(
        () ->
            applyValidationAndComputed(
                tableMetadata.getColumns(), new Row("Name", "pietje"), db.getJooq()));
  }

  @Test
  void testCheckValidationWithCapitalColumnNameFail() {
    String validation = "name === 'pietje'";
    TableMetadata tableMetadata = table("Test", new Column("Name").setValidation(validation));
    List<Column> columns = tableMetadata.getColumns();
    Row row = new Row("Name", "piet");
    assertThrows(
        MolgenisException.class, () -> applyValidationAndComputed(columns, row, db.getJooq()));
  }

  @Tag("windowsFail")
  @Test
  void testCheckValidationInvalidExpression() {
    String validation = "this is very invalid";
    List<Column> columns = table("Test", new Column("name").setValidation(validation)).getColumns();
    Row row = new Row("name", "test");
    String expected =
        """
        script failed: SyntaxError: Unnamed:1:5 Expected ; but found is
        this is very invalid\\n
             ^\\n,
        """;
    assertThrows(
        MolgenisException.class,
        () -> applyValidationAndComputed(columns, row, db.getJooq()),
        expected);
  }

  @Test
  void givenInvalidArgumentForAutoIdComputed_thenThrowException() {
    Table table =
        schema.create(
            table(
                "test_invalid_autoid",
                new Column("id").setType(ColumnType.AUTO_ID).setComputed("${mg_autoid(invalid)}")));

    List<Column> columns = table.getMetadata().getColumns();
    assertThrows(
        MolgenisException.class,
        () -> applyValidationAndComputed(columns, new Row(), db.getJooq()));
  }

  @Test
  void givenArgumentForAutoIdComputed_thenGenerateAccordingly() {
    TableMetadata table =
        schema
            .create(
                table(
                    "test_autoid",
                    new Column("id")
                        .setType(ColumnType.AUTO_ID)
                        .setComputed("${mg_autoid(length=1, format=NUMBERS)}")))
            .getMetadata();

    List<Column> columns = table.getColumns();

    Row row = new Row();
    applyValidationAndComputed(columns, row, db.getJooq());
    String generatedId = row.get("id", String.class);
    Matcher matcher = Pattern.compile("\\d").matcher(generatedId);
    assertTrue(matcher.find());
  }

  @Test
  void testCheckValidationTurnToBoolIsFalse() {
    String validation = "false";
    TableMetadata tableMetadata = table("Test", new Column("name").setValidation(validation));
    try {
      applyValidationAndComputed(tableMetadata.getColumns(), new Row("name", "test"), db.getJooq());
    } catch (MolgenisException exception) {
      assertEquals("Validation error on column 'name': false.", exception.getMessage());
    }
  }

  @Test
  void testInvisibleAreNotUpdated() {
    Table test1 =
        schema.create(
            table(
                "Test1",
                column("id").setPkey(),
                column("name"),
                column("age")
                    .setValidation("age > 10")
                    .setVisible("name")
                    .setType(ColumnType.INT)));

    // if no name then age should be skipped
    test1.insert(row("id", 1, "age", 11));
    assertNull(test1.retrieveRows().get(0).getInteger("age"));

    // if  name then age should be included
    test1.insert(row("id", 2, "name", "foo", "age", 11));
    assertEquals(11L, (long) test1.retrieveRows().get(1).getInteger("age"));

    try {
      test1.insert(row("id", 3, "name", "foo2", "age", 10));
      fail("should have failed");
    } catch (Exception e) {
      // correct, should be age>10
      assertTrue(e.getMessage().contains("age > 10"));
    }

    // simple foreign key
    Table test2 =
        schema.create(
            table(
                "Test2",
                column("id").setPkey(),
                column("ref")
                    .setType(ColumnType.REF)
                    .setRefTable("Test1")
                    .setValidation("ref.id == 1"),
                column("refArray")
                    .setType(ColumnType.REF_ARRAY)
                    .setRefTable("Test1")
                    .setValidation("refArray.some(r => r.id == 1)")));

    // should fail on ref
    try {
      test2.insert(row("id", 1, "ref", 2, "refArray", 1));
      fail("should fail");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Validation error"));
      assertTrue(e.getMessage().contains("ref.id == 1"));
    }

    // should fail on refArray
    try {
      test2.insert(row("id", 1, "ref", 1, "refArray", 2));
      fail("should fail");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Validation error"));
      assertTrue(e.getMessage().contains("refArray.some(r => r.id == 1)"));
    }

    test2.insert(row("id", 1, "ref", 1, "refArray", List.of(1)));

    // composite foreign key and computed
    Table test3 =
        schema.create(
            table(
                "Test3",
                column("first name").setPkey(),
                column("last name").setPkey(),
                column("display name").setComputed("firstName+' '+lastName")));
    test3.insert(row("first name", "foo", "last name", "bar"));
    assertEquals("foo bar", test3.retrieveRows().get(0).getString("display name"));

    Table test4 =
        schema.create(
            table(
                "Test4",
                column("id").setPkey(),
                column("ref")
                    .setType(ColumnType.REF)
                    .setRefTable("Test3")
                    .setValidation("ref.firstName == 'foo' && ref.lastName == 'bar'"),
                column("refArray")
                    .setType(ColumnType.REF_ARRAY)
                    .setRefTable("Test3")
                    .setValidation(
                        "refArray.some(r => r.firstName == 'foo' && r.lastName == 'bar')")));

    try {
      test4.insert(
          row(
              "id",
              1,
              "ref.first name",
              "fo2",
              "ref.last name",
              "ba2",
              "refArray.first name",
              "foo",
              "refArray.last name",
              "bar"));
      fail("should fail");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Validation error"));
      assertTrue(e.getMessage().contains("ref.firstName == 'foo' && ref.lastName == 'bar'"));
    }

    // should fail on refArray
    try {
      test4.insert(
          row(
              "id",
              1,
              "ref.first name",
              "foo",
              "ref.last name",
              "bar",
              "refArray.first name",
              "fo2",
              "refArray.last name",
              "ba2"));
      fail("should fail");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Validation error"));
      assertTrue(
          e.getMessage()
              .contains("refArray.some(r => r.firstName == 'foo' && r.lastName == 'bar')"));
    }

    test4.insert(
        row(
            "id",
            1,
            "ref.first name",
            "foo",
            "ref.last name",
            "bar",
            "refArray.first name",
            "foo",
            "refArray.last name",
            "bar"));
  }
}
