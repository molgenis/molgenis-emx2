package org.molgenis.sql;

import org.junit.BeforeClass;
import org.junit.Test;
import org.molgenis.*;

import static org.molgenis.Column.Type.*;

public class TestRefs {

  static Database db;

  @BeforeClass
  public static void setup() throws MolgenisException {
    db = DatabaseFactory.getDatabase("molgenis", "molgenis");
  }

  @Test
  public void testInt() throws MolgenisException {
    executeTest(INT, 5, 6);
  }

  @Test
  public void testString() throws MolgenisException {
    executeTest(STRING, "test", "test2");
  }

  @Test
  public void testDate() throws MolgenisException {
    executeTest(DATE, "2013-01-01", "2013-01-02");
  }

  @Test
  public void testDateTime() throws MolgenisException {
    executeTest(DATETIME, "2013-01-01T18:00:00", "2013-01-01T18:00:01");
  }

  @Test
  public void testDecimal() throws MolgenisException {
    executeTest(DECIMAL, 5.0, 6.0);
  }

  @Test
  public void testText() throws MolgenisException {
    executeTest(TEXT, "This is a hello world", "This is a hello back to you");
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(
        UUID, "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4", "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce5");
  }

  private void executeTest(Column.Type type, Object insertValue, Object updateValue)
      throws MolgenisException {

    Schema s = db.createSchema("TestRefs" + type.toString().toUpperCase());

    Table a = s.createTable("A");
    String fieldName = type + "Col";
    a.addColumn(fieldName, type);
    a.addUnique(fieldName);
    Row aRow = new Row().set(fieldName, insertValue);
    a.insert(aRow);

    Table b = s.createTable("B");
    String refName = type + "Ref";
    b.addRef(refName, "A", fieldName);
    b.insert(new Row().set(refName, insertValue));

    // and update, should be cascading!
    a.update(aRow.set(fieldName, updateValue));
  }
}
