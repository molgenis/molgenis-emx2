package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
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
import org.molgenis.emx2.Column;
import org.molgenis.emx2.ColumnType;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Order;
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
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, query, "testTable");
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
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, from, "testTable");
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by ((lower(\"testColumn\")) collate \"MOLGENIS\".numeric) asc",
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
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, from, "testTable");
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
        SqlQueryBuilderHelpers.orderBy(tableMetadata, select, from, "testTable");
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by ((lower(\"testColumn\")) collate \"MOLGENIS\".numeric) desc",
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
            tableMetadata, select, jooq.select().from(tableMetadata.getJooqTable()), "testTable");
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by ((lower(\"refColumn\")) collate \"MOLGENIS\".numeric) asc",
        ascQuery.getSQL());

    final SelectColumn selectDesc = new SelectColumn("refColumn");
    selectDesc.setOrderBy(Collections.singletonMap("refColumn", Order.DESC));
    SelectConnectByStep<Record> descQuery =
        SqlQueryBuilderHelpers.orderBy(
            tableMetadata,
            selectDesc,
            jooq.select().from(tableMetadata.getJooqTable()),
            "testTable");
    assertEquals(
        "select * from \"SqlQueryBuilderHelpersTest\".\"testTable\" order by ((lower(\"refColumn\")) collate \"MOLGENIS\".numeric) desc",
        descQuery.getSQL());
  }

  @Test
  void getNonExistingColumn() {
    final TableMetadata table = testTable.getMetadata();
    assertThrows(MolgenisException.class, () -> table.getQueryColumn("foo"));
  }

  @Test
  void getSearchColumn() {
    final TableMetadata table = new TableMetadata("testTable");
    final Column column = table.getQueryColumn(TEXT_SEARCH_COLUMN_NAME);
    assertEquals("testTable" + TEXT_SEARCH_COLUMN_NAME, column.getName());
    assertNull(column.getOrigin());
  }

  @Test
  void getColumnByName() {
    final Column column = testTable.getMetadata().getQueryColumn("testColumn");
    assertEquals("testColumn", column.getName());
    assertNull(column.getOrigin());
  }

  @Test
  void getRefKeyPartCarriesItsOrigin() {
    testSchema.create(new TableMetadata("refTable").add(new Column("refColumn").setPkey()));
    final TableMetadata tableMetadata = testTable.getMetadata();
    tableMetadata.add(new Column("ref1").setType(ColumnType.ONTOLOGY).setRefTable("refTable"));
    final Column ref = tableMetadata.getColumn("ref1");
    final Column keyPart = ref.getReferences().get(0).toPrimitiveColumn();
    assertEquals(ColumnType.STRING, keyPart.getColumnType());
    assertEquals(ref, keyPart.getOrigin());
    assertEquals(ColumnType.ONTOLOGY, keyPart.getOrigin().getColumnType());
    assertNull(tableMetadata.getQueryColumn("ref1").getOrigin());
  }

  @Test
  void getFileAttributeCarriesItsOrigin() {
    final TableMetadata table = testTable.getMetadata();
    table.add(new Column("col1").setType(ColumnType.FILE));
    final Column column = table.getQueryColumn("col1_contents");
    assertEquals("col1_contents", column.getName());
    assertEquals(ColumnType.STRING, column.getColumnType());
    assertEquals("col1", column.getOrigin().getName());
  }
}
