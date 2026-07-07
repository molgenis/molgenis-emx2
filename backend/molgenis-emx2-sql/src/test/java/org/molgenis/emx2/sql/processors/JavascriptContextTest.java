package org.molgenis.emx2.sql.processors;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.Row.row;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Row;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.sql.JavascriptContextBuilder;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class JavascriptContextTest {

  private static Schema schema;

  @BeforeAll
  static void setUp() {
    schema =
        TestDatabaseFactory.getTestDatabase()
            .dropCreateSchema(JavascriptContextTest.class.getSimpleName());
    schema.create(table("Category", column("id", ColumnType.INT).setPkey()));
    schema.create(
        table(
            "Item",
            column("id").setPkey(),
            column("category").setType(ColumnType.REF).setRefTable("Category"),
            column("tags").setType(ColumnType.REF_ARRAY).setRefTable("Category")));
  }

  @Test
  void testArrayConversionToMap() {
    List<Column> columns = List.of(column("STRING array", ColumnType.STRING_ARRAY));
    Row row = row("STRING array", "aa,bb");

    Map<String, Object> context = JavascriptContextBuilder.fromRow(columns, row);

    assertAll(
        () -> assertEquals(Set.of("sTRINGArray"), context.keySet()),
        () ->
            assertEquals(
                List.of("aa", "bb"), Arrays.asList((String[]) context.get("sTRINGArray"))));
  }

  @Test
  void testScalarColumnMapsToIdentifier() {
    List<Column> columns = List.of(column("first name", ColumnType.STRING));
    Row row = row("first name", "Alice");

    Map<String, Object> context = JavascriptContextBuilder.fromRow(columns, row);

    assertEquals("Alice", context.get("firstName"));
  }

  @Test
  void testFileColumnIsExcludedFromContext() {
    List<Column> columns = List.of(column("profile image", ColumnType.FILE));
    Row row = row();

    Map<String, Object> context = JavascriptContextBuilder.fromRow(columns, row);

    assertFalse(context.containsKey("profileImage"));
  }

  @Test
  void testNullValueIsPresentInContext() {
    List<Column> columns = List.of(column("count", ColumnType.INT));
    Row row = row();

    Map<String, Object> context = JavascriptContextBuilder.fromRow(columns, row);

    assertTrue(context.containsKey("count"));
    assertNull(context.get("count"));
  }

  @Test
  void testEmptyColumnListYieldsEmptyContext() {
    Map<String, Object> context = JavascriptContextBuilder.fromRow(List.of(), row());

    assertTrue(context.isEmpty());
  }

  @Test
  void testRefColumnProducesNestedMap() {
    Column column = schema.getTable("Item").getMetadata().getColumn("category");
    Row row = row("category", 42);

    Map<String, Object> context = JavascriptContextBuilder.fromRow(List.of(column), row);

    assertInstanceOf(Map.class, context.get("category"));
    assertEquals(42, ((Map<?, ?>) context.get("category")).get("id"));
  }

  @Test
  void testRefArrayColumnProducesListOfMaps() {
    Column column = schema.getTable("Item").getMetadata().getColumn("tags");
    Row row = row("tags", "1,2,3");

    Map<String, Object> context = JavascriptContextBuilder.fromRow(List.of(column), row);

    assertInstanceOf(List.class, context.get("tags"));
    List<?> list = (List<?>) context.get("tags");
    assertAll(
        () -> assertEquals(3, list.size()),
        () -> assertEquals(1, ((Map<?, ?>) list.get(0)).get("id")),
        () -> assertEquals(2, ((Map<?, ?>) list.get(1)).get("id")),
        () -> assertEquals(3, ((Map<?, ?>) list.get(2)).get("id")));
  }
}
