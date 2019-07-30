// package org.molgenis.sql;
//
// import org.junit.BeforeClass;
// import org.junit.Test;
// import org.molgenis.*;
//
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.List;
//
// import static junit.framework.TestCase.fail;
// import static org.molgenis.Type.*;
//
// public class TestMrefs {
//
//  static Database db;
//
//  @BeforeClass
//  public static void setup() throws MolgenisException {
//    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
//  }
//
//  @Test
//  public void testString() throws MolgenisException {
//    executeTest(STRING, new String[] {"aap", "noot", "mies"});
//  }
//
//  //    @Test
//  //    public void testInt() throws MolgenisException {
//  //        executeTest(INT, 5, 6);
//  //    }
//  //
//  //    @Test
//  //    public void testDate() throws MolgenisException {
//  //        executeTest(DATE, "2013-01-01", "2013-01-02");
//  //    }
//  //
//  //    @Test
//  //    public void testDateTime() throws MolgenisException {
//  //        executeTest(DATETIME, "2013-01-01T18:00:00", "2013-01-01T18:00:01");
//  //    }
//  //
//  //    @Test
//  //    public void testDecimal() throws MolgenisException {
//  //        executeTest(DECIMAL, 5.0, 6.0);
//  //    }
//  //
//  //    @Test
//  //    public void testText() throws MolgenisException {
//  //        executeTest(TEXT, "This is a hello world", "This is a hello back to you");
//  //    }
//  //
//  //    @Test
//  //    public void testUUID() throws MolgenisException {
//  //        executeTest(
//  //                UUID, "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4",
//  // "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5");
//  //    }
//
//  private void executeTest(Type type, Object[] values) throws MolgenisException {
//
//    Schema s = db.createSchema("TestMrefs" + type.toString().toUpperCase());
//
//    Table a = s.createTable("A");
//    String aFieldName = type + "Col";
//    a.addColumn(aFieldName, type);
//    a.addUnique(aFieldName);
//
//    List<Row> aRows = new ArrayList<>();
//    for (Object value : values) {
//      Row aRow = new Row().set(aFieldName, value);
//      a.insert(aRow);
//      aRows.add(aRow);
//    }
//
//    Table b = s.createTable("B");
//    String refName = type + "Ref";
//    b.addMref(refName, "A", aFieldName);
//
//    // link table "MREF|B|A|typeRef" {
//    //    molgenisid uuid
//    //    refName ref(B.molgenisid) //you can set this via MREF from A->B
//    //    aFieldName ref(A.aFieldName))
//    // }
//
//    Row bRow = new Row().set(refName, Arrays.copyOfRange(values, 1, 3));
//    b.insert(bRow);
//
//    // and update
//    bRow.set(refName, Arrays.copyOfRange(values, 0, 2));
//    b.update(bRow);
//
//    // delete of referenced A should fail
//    try {
//      a.delete(aRows.get(0));
//      fail("delete should fail");
//    } catch (Exception e) {
//      System.out.println("delete exception correct: " + e.getMessage());
//    }
//    // delete in dependency order should succeed
//    b.delete(bRow);
//    for (Row aRow : aRows) {
//      a.delete(aRow);
//    }
//  }
// }
