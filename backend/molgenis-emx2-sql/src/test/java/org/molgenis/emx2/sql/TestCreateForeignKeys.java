package org.molgenis.emx2.sql;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.Column.column;
import static org.molgenis.emx2.ColumnType.*;
import static org.molgenis.emx2.FilterBean.f;
import static org.molgenis.emx2.Operator.*;
import static org.molgenis.emx2.TableMetadata.table;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;

class TestCreateForeignKeys {

  static Database db;

  @BeforeAll
  static void setup() {
    db = TestDatabaseFactory.getTestDatabase();
  }

  @Test
  void testInt() {
    executeTest(INT, 5, 6);
  }

  @Test
  void testString() {
    executeTest(STRING, "test", "DependencyOrderOutsideTransactionFails");
  }

  @Test
  void testDate() {
    executeTest(DATE, "2013-01-01", "2013-01-02");
  }

  @Test
  void testDateTime() {
    executeTest(DATETIME, "2013-01-01T18:00:00", "2013-01-01T18:00:01");
  }

  @Test
  void testDecimal() {
    executeTest(DECIMAL, 5.0, 6.0);
  }

  @Test
  void testText() {
    executeTest(TEXT, "This is a hello world", "This is a hello back to you");
  }

  @Test
  void testAutoId() {
    executeTest(AUTO_ID, "test-auto-id", "test-another-auto-id4");
  }

  @Test
  void testUUID() {
    executeTest(
        UUID, "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4", "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5");
  }

  @Test
  void testJSON() {
    executeTest(JSON, "{\"key\": \"value1\"}", "{\"key\": \"value2\"}");
  }

  private void executeTest(ColumnType columnType, Object insertValue, Object updateValue) {

    Schema schema =
        db.dropCreateSchema("TestCreateForeignKeys" + columnType.toString().toUpperCase());

    String fieldName = "AKeyOf" + columnType;
    Table aTable = schema.create(table("A").add(column(fieldName).setType(columnType).setPkey()));
    Row aRow = new Row().set(fieldName, insertValue);
    aTable.insert(aRow);

    String refFromBToA = "RefToAKeyOf" + columnType;
    String refFromBToANillable = refFromBToA + "Nilable";
    Table bTable =
        schema.create(
            table("B")
                .add(column("ID").setType(INT).setPkey())
                .add(column(refFromBToA).setType(REF).setRefTable("A").setRequired(true))
                .add(column(refFromBToANillable).setType(REF).setRefTable("A")));
    Row bRow = new Row().setInt("ID", 2).set(refFromBToA, insertValue);
    bTable.insert(bRow);

    // insert to non-existing value should fail
    Row bErrorRow = new Row().setInt("ID", 3).set(refFromBToA, updateValue);
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because value is not in other table");
    } catch (Exception e) {
      System.out.println("insert exception correct: \n" + e);
    }

    // insert null should fail
    bErrorRow = new Row().setInt("ID", 3).set(refFromBToA, null);
    try {
      bTable.insert(bErrorRow);
      fail("insert should fail because value is null");
    } catch (Exception e) {
      System.out.println("insert exception correct: \n" + e);
    }

    // and update, should be cascading :-)
    // aTable.update(aRow.set(fieldName, updateValue));
    // big todo: implement update of pkey columns

    // delete of A should fail
    try {
      aTable.delete(aRow);
      fail("delete should fail because bRow is still referencing aRow");
    } catch (Exception e) {
      System.out.println("insert exception correct: \n" + e);
    }

    // filter on null/not null
    List<Row> result = bTable.where(f(refFromBToA, IS_NULL, true)).retrieveRows();
    assertEquals(0, result.size());
    result = bTable.where(f(refFromBToANillable, IS_NULL, true)).retrieveRows();
    assertEquals(1, result.size());

    result = bTable.where(f(refFromBToA, IS_NULL, false)).retrieveRows();
    assertEquals(1, result.size());
    result = bTable.where(f(refFromBToANillable, IS_NULL, false)).retrieveRows();
    assertEquals(0, result.size());

    result = bTable.where(f(refFromBToA, MATCH_ANY, insertValue, updateValue)).retrieveRows();
    assertEquals(1, result.size());

    bTable.delete(bRow);
    aTable.delete(aRow);
  }

  @Test
  void givenOptionalAutoIdReference_whenAutoIdIsNull_thenLeaveBlank() {
    Schema schema = db.dropCreateSchema("TestCreateForeignKeysAutoId");
    schema.create(table("A").add(column("id").setType(AUTO_ID).setPkey()));
    Table tableB =
        schema.create(
            table("B")
                .add(column("id").setType(INT).setPkey())
                .add(column("a").setType(REF).setRefTable("A").setRequired(false)));
    Row row = new Row().setInt("id", 2);
    tableB.insert(row);

    Row result = tableB.retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS).getFirst();
    assertNull(result.get("a", AUTO_ID));
  }
}
