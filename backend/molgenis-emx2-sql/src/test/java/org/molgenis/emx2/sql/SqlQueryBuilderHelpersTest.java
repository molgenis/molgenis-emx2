package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.molgenis.emx2.Constants.TEXT_SEARCH_COLUMN_NAME;

import java.util.Collections;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectConnectByStep;
import org.jooq.SelectJoinStep;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Order;
import org.molgenis.emx2.Reference;
import org.molgenis.emx2.SelectColumn;
import org.molgenis.emx2.TableMetadata;

class SqlQueryBuilderHelpersTest {

  private static Database testDb;
  private SqlSchema testSchema;

  private SqlTable testTable;

  private static String testSchemaName = "SqlQueryBuilderHelpersTest";

  @BeforeAll
  static void beforeAll() {
    testDb = TestDatabaseFactory.getTestDatabase();
  }

  @BeforeEach
  void beforeEach() {
    testDb.dropSchemaIfExists(testSchemaName);
    testSchema = (SqlSchema) testDb.createSchema(testSchemaName);
    testTable =
        (SqlTable)
            testSchema.create(
                new TableMetadata("testTable")
                    .add(new Column("testColumn"))
                    .add(new Column("testColumn2").setType(ColumnType.INT)));
  }

  @AfterAll()
  static void afterAll() {
    testDb.dropSchemaIfExists(testSchemaName);
  }

  @Test
  void orderByWithoutOrderByInSelect() {

    final SqlTableMetadata tableMetadata = testTable.getMetadata();
    final DSLContext jooq = tableMetadata.getJooq();

    final SelectColumn select = new SelectColumn("testColumn");
    SelectJoinStep<org.jooq.Record> query = jooq.select().from(tableMetadata.getJooqTable());

    SelectConnectByStep<Record> resultQuery =
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, query);
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\"", resultQuery.getSQL());
  }

  @Test
  void orderByASC() {
    final SqlTableMetadata tableMetadata = testTable.getMetadata();
    final DSLContext jooq = tableMetadata.getJooq();

    SelectJoinStep<org.jooq.Record> from = jooq.select().from(tableMetadata.getJooqTable());

    final SelectColumn select = new SelectColumn("testColumn");
    select.setOrderBy(Collections.singletonMap("testColumn", Order.ASC));
    SelectConnectByStep<Record> ascQuery =
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, from);
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by lower(\"testColumn\") asc",
        ascQuery.getSQL());
  }

  @Test
  void orderByOnNonCaseSensitiveColumn() {
    final SqlTableMetadata tableMetadata = testTable.getMetadata();
    final DSLContext jooq = tableMetadata.getJooq();

    SelectJoinStep<org.jooq.Record> from = jooq.select().from(tableMetadata.getJooqTable());

    final SelectColumn select = new SelectColumn("testColumn2");
    select.setOrderBy(Collections.singletonMap("testColumn2", Order.ASC));
    SelectConnectByStep<Record> ascQuery =
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, from);
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by \"testColumn2\" asc",
        ascQuery.getSQL());
  }

  @Test
  void orderByDESC() {
    final SqlTableMetadata tableMetadata = testTable.getMetadata();
    final DSLContext jooq = tableMetadata.getJooq();

    SelectJoinStep<org.jooq.Record> from = jooq.select().from(tableMetadata.getJooqTable());

    final SelectColumn select = new SelectColumn("testColumn");
    select.setOrderBy(Collections.singletonMap("testColumn", Order.DESC));
    SelectConnectByStep<Record> ascQuery =
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, from);
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by lower(\"testColumn\") desc",
        ascQuery.getSQL());
  }

  @Test
  void orderByRef() {
    testSchema.create(new TableMetadata("refTable").add(new Column("refColumn").setPkey()));
    final SqlTableMetadata tableMetadata = testTable.getMetadata();
    tableMetadata.add(new Column("refColumn").setType(ColumnType.REF).setRefTable("refTable"));
    final DSLContext jooq = tableMetadata.getJooq();

    final SelectColumn select = new SelectColumn("refColumn");
    select.setOrderBy(Collections.singletonMap("refColumn", Order.ASC));
    SelectConnectByStep<Record> ascQuery =
        SqlQueryBuilderHelpers.orderBy(
            tableMetadata, select, jooq.select().from(tableMetadata.getJooqTable()));
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by lower(\"refColumn\") asc",
        ascQuery.getSQL());

    final SelectColumn selectDesc = new SelectColumn("refColumn");
    selectDesc.setOrderBy(Collections.singletonMap("refColumn", Order.DESC));
    SelectConnectByStep<Record> descQuery =
        SqlQueryBuilderHelpers.orderBy(
            tableMetadata, selectDesc, jooq.select().from(tableMetadata.getJooqTable()));
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by lower(\"refColumn\") desc",
        descQuery.getSQL());
  }

  @Test
  void getNonExistingColumn() {
    final TableMetadata table = new TableMetadata("testTable");
    assertThrows(
        MolgenisException.class, () -> SqlQueryBuilderHelpers.getColumnByName(table, "foo"));
  }

  @Test
  void getSearchColumn() {
    final TableMetadata table = new TableMetadata("testTable");
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, TEXT_SEARCH_COLUMN_NAME);
    assertEquals("testTable" + TEXT_SEARCH_COLUMN_NAME, column.getName());
  }

  @Test
  void getColumnByName() {
    final TableMetadata table = new TableMetadata("testTable");
    table.add(new Column("col1"));
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, "col1");
    assertEquals("col1", column.getName());
  }

  @Test
  void getRefColumnByName() {
    final TableMetadata table = Mockito.mock(TableMetadata.class);
    final Column col1 = Mockito.mock(Column.class);
    final Reference ref1 = Mockito.mock(Reference.class);
    when(ref1.getName()).thenReturn("ref1");
    when(col1.getName()).thenReturn("col1");
    when(col1.getReferences()).thenReturn(Collections.singletonList(ref1));
    when(table.getColumns()).thenReturn(Collections.singletonList(col1));
    when(table.getTableName()).thenReturn("testTable");
    when(ref1.getPrimitiveType()).thenReturn(ColumnType.REF);

    table.add(col1);
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, "ref1");
    assertEquals("ref1", column.getName());
  }

  @Test
  void getFileColumnByName() {
    final TableMetadata table = Mockito.mock(TableMetadata.class);
    when(table.getTableName()).thenReturn("testTable");
    final Column col1 = Mockito.mock(Column.class);
    when(col1.getName()).thenReturn("col1_contents");
    when(col1.isFile()).thenReturn(true);
    when(table.getColumns()).thenReturn(Collections.singletonList(col1));

    table.add(col1);
    final Column column = SqlQueryBuilderHelpers.getColumnByName(table, "col1_contents");
    assertEquals("col1_contents", column.getName());
  }
}
