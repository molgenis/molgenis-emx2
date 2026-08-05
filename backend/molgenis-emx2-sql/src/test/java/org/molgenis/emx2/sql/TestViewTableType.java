package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

public class TestViewTableType {

  private static Database db;

  @BeforeAll
  public static void setUp() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  public void testCreateAndQueryView() {
    Schema s = db.dropCreateSchema(TestViewTableType.class.getSimpleName());

    // Create a base table with some data
    s.create(
        table("Products", column("id").setType(ColumnType.INT).setPkey(), column("name").setType(ColumnType.STRING), column("price").setType(ColumnType.DECIMAL)));

    Table products = s.getTable("Products");
    products.insert(new Row().setInt("id", 1).setString("name", "Apple").setDecimal("price", 1.5));
    products.insert(new Row().setInt("id", 2).setString("name", "Banana").setDecimal("price", 0.75));
    products.insert(new Row().setInt("id", 3).setString("name", "Cherry").setDecimal("price", 3.0));

    // Create a VIEW on that table
    String viewSql =
        "SELECT id, name, price FROM \""
            + TestViewTableType.class.getSimpleName()
            + "\".\"Products\" WHERE price > 1.0";
    s.create(
        table("ExpensiveProducts")
            .setTableType(TableType.VIEW)
            .setViewSql(viewSql));

    // Verify the view table metadata
    TableMetadata viewMeta = s.getTable("ExpensiveProducts").getMetadata();
    assertNotNull(viewMeta);
    assertEquals(TableType.VIEW, viewMeta.getTableType());
    assertEquals(viewSql, viewMeta.getViewSql());

    // Verify the view was created and returns rows via raw SQL query
    List<Row> rows =
        s.retrieveSql(
            "SELECT * FROM \""
                + TestViewTableType.class.getSimpleName()
                + "\".\"ExpensiveProducts\"");
    assertEquals(2, rows.size(), "View should return only products with price > 1.0");

    // Verify dropping the view works
    s.dropTable("ExpensiveProducts");
    assertNull(s.getTable("ExpensiveProducts"), "View should be removed after drop");
  }

  @Test
  public void testCreateViewWithoutSqlThrowsException() {
    Schema s = db.dropCreateSchema(TestViewTableType.class.getSimpleName() + "2");

    MolgenisException ex =
        assertThrows(
            MolgenisException.class,
            () -> s.create(table("MyView").setTableType(TableType.VIEW)),
            "Creating a VIEW without viewSql should throw an exception");
    assertTrue(
        ex.getMessage().contains("viewSql must be provided"),
        "Exception message should mention viewSql");
  }
}
