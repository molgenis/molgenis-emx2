// package org.molgenis.emx2.sql;
//
// import org.junit.jupiter.api.BeforeAll;
// import org.junit.jupiter.api.Test;
// import org.molgenis.emx2.*;
// import org.molgenis.emx2.ColumnType;
// import org.molgenis.emx2.utils.StopWatch;
// import org.molgenis.emx2.utils.TypeUtils;
//
// import java.io.Serializable;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
//
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.molgenis.emx2.Column.column;
// import static org.molgenis.emx2.ColumnType.*;
// import static org.molgenis.emx2.Operator.EQUALS;
// import static org.molgenis.emx2.TableMetadata.table;
//
// public class TestCreateMREF {
//
//  static Database db;
//
//  @BeforeAll
//  public static void setup() {
//    db = TestDatabaseFactory.getTestDatabase();
//  }
//
//  @Test
//  public void testUUID_MREF() {
//    executeTest(
//        UUID,
//        new java.util.UUID[] {
//          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
//          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
//          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
//        });
//  }
//
//  @Test
//  public void testString_MREF() {
//    executeTest(STRING, new String[] {"aap", "noot", "mies"});
//  }
//
//  @Test
//  public void testInt_MREF() {
//    executeTest(INT, new Integer[] {5, 6, 7});
//  }
//
//  @Test
//  public void testDate_MREF() {
//    executeTest(DATE, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
//  }
//
//  @Test
//  public void testDateTime_MREF() {
//    executeTest(
//        DATETIME,
//        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
//  }
//
//  @Test
//  public void testDecimal_MREF() {
//    executeTest(DECIMAL, new Double[] {5.0, 6.0, 7.0});
//  }
//
//  @Test
//  public void testText_MREF() {
//    executeTest(
//        TEXT,
//        new String[] {
//          "This is a hello world", "This is a hello back to you", "This is a hello some more"
//        });
//  }
//
//  private void executeTest(ColumnType columnType, Serializable[] testValues) {
//    StopWatch.start("executeTest");
//
//    Schema aSchema =
//        db.createSchema("TestCreateManyToManyRelations" + columnType.toString().toUpperCase());
//
//    String keyOfA = "AKey";
//    Table aTable = aSchema.create(table("A").add(column(keyOfA).type(columnType)).pkey(keyOfA));
//
//    String keyOfB = "BKey";
//    Table bTable = aSchema.create(table("B").add(column(keyOfB)).pkey(keyOfB));
//
//    StopWatch.print("schema created");
//
//    List<Row> aRowList = new ArrayList<>();
//    for (Object value : testValues) {
//      Row aRow = new Row().set(keyOfA, value);
//      aTable.insert(aRow);
//      aRowList.add(aRow);
//    }
//
////    // add one sided many-to-many
////    String refName = columnType + "refToA";
////    bTable.getMetadata().add(column(refName).type(MREF).refTable("A").refColumn(keyOfA));
//
//    //    String refReverseName = columnType + "refToB";
//    // refReverseName, keyOfB
//
////    Row bRow =
////        new Row().set(keyOfB, keyOfB + "1").set(refName, Arrays.copyOfRange(testValues, 1, 3));
////    bTable.insert(bRow);
//
//    StopWatch.print("data inserted");
//
//    // test query
//    List<Row> bRowsRetrieved = bTable.filter(refName, EQUALS, testValues[1]).getRows();
//    ColumnType arrayColumnType = TypeUtils.getArrayType(columnType);
//
//    // todo insert order is not reproducible
//    List first = Arrays.asList((Object[]) bRow.get(refName, arrayColumnType));
//    List second = Arrays.asList((Object[]) bRowsRetrieved.get(0).get(refName, arrayColumnType));
//    assertTrue(
//        first.size() == second.size() && first.containsAll(second) && second.containsAll(first));
//
//    // and update
//    bRow.set(refName, Arrays.copyOfRange(testValues, 0, 2));
//    bTable.update(bRow);
//
//    StopWatch.print("data updated");
//
//    bTable.delete(bRow);
//    for (Row aRow : aRowList) {
//      aTable.delete(aRow);
//    }
//
//    StopWatch.print("data deleted");
//  }
// }
