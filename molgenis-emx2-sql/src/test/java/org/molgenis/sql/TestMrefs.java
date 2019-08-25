package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;
import org.molgenis.data.Database;
import org.molgenis.data.Row;
import org.molgenis.data.Table;
import org.molgenis.metadata.Type;
import org.molgenis.data.Schema;
import org.molgenis.utils.StopWatch;
import org.molgenis.utils.TypeUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.molgenis.metadata.Type.*;

public class TestMrefs {

  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getTestDatabase("molgenis", "molgenis");
  }

  @Test
  public void testUUID_MREF() throws MolgenisException {
    executeTest(
        UUID,
        new java.util.UUID[] {
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5"),
          java.util.UUID.fromString("f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce6")
        });
  }

  @Test
  public void testString_MREF() throws MolgenisException {
    executeTest(STRING, new String[] {"aap", "noot", "mies"});
  }

  @Test
  public void testInt_MREF() throws MolgenisException {
    executeTest(INT, new Integer[] {5, 6, 7});
  }

  @Test
  public void testDate_MREF() throws MolgenisException {
    executeTest(DATE, new String[] {"2013-01-01", "2013-01-02", "2013-01-03"});
  }

  @Test
  public void testDateTime_MREF() throws MolgenisException {
    executeTest(
        DATETIME,
        new String[] {"2013-01-01T18:00:00", "2013-01-01T18:00:01", "2013-01-01T18:00:02"});
  }

  @Test
  public void testDecimal_MREF() throws MolgenisException {
    executeTest(DECIMAL, new Double[] {5.0, 6.0, 7.0});
  }

  @Test
  public void testText_MREF() throws MolgenisException {
    executeTest(
        TEXT,
        new String[] {
          "This is a hello world", "This is a hello back to you", "This is a hello some more"
        });
  }

  private void executeTest(Type type, Object[] testValues) throws MolgenisException {
    StopWatch.start("executeTest");

    Schema aSchema = db.createSchema("TestMrefs" + type.toString().toUpperCase());

    Table aTable = aSchema.createTableIfNotExists("A");
    String keyOfA = "AKey";
    aTable.getMetadata().addColumn(keyOfA, type);
    aTable.getMetadata().addUnique(keyOfA);

    Table bTable = aSchema.createTableIfNotExists("B");
    String keyOfB = "BKey";
    bTable.getMetadata().addColumn(keyOfB, STRING);
    bTable.getMetadata().addUnique(keyOfB);

    StopWatch.print("schema created");

    List<Row> aRowList = new ArrayList<>();
    for (Object value : testValues) {
      Row aRow = new Row().set(keyOfA, value);
      aTable.insert(aRow);
      aRowList.add(aRow);
    }

    // add two sided many-to-many
    String refName = type + "refToA";
    String refReverseName = type + "refToB";
    String joinTableName = "AB";
    bTable.getMetadata().addMref(refName, "A", keyOfA, refReverseName, keyOfB, joinTableName);

    Row bRow =
        new Row().set(keyOfB, keyOfB + "1").set(refName, Arrays.copyOfRange(testValues, 1, 3));
    bTable.insert(bRow);

    StopWatch.print("data inserted");

    // test query
    List<Row> bRowsRetrieved = bTable.retrieve();
    Type arrayType = TypeUtils.getArrayType(type);
    assertArrayEquals(
        (Object[]) bRow.get(arrayType, refName),
        (Object[]) bRowsRetrieved.get(0).get(arrayType, refName));

    // and update
    bRow.set(refName, Arrays.copyOfRange(testValues, 0, 2));
    bTable.update(bRow);

    StopWatch.print("data updated");

    bTable.delete(bRow);
    for (Row aRow : aRowList) {
      aTable.delete(aRow);
    }

    StopWatch.print("data deleted");
  }
}
