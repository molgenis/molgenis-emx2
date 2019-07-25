package org.molgenis.sql;

import org.junit.Test;
import org.molgenis.*;

import static org.molgenis.Column.Type.*;

public class TestRefs {

  @Test
  public void testInt() throws MolgenisException {
    executeTest(INT, 5);
  }

  @Test
  public void testString() throws MolgenisException {
    executeTest(STRING, "test");
  }

  @Test
  public void testDate() throws MolgenisException {
    executeTest(DATE, "2013-01-01");
  }

  @Test
  public void testDateTime() throws MolgenisException {
    executeTest(DATETIME, "2013-01-01T18:00:00");
  }

  @Test
  public void testDecimal() throws MolgenisException {
    executeTest(DECIMAL, 5.0);
  }

  @Test
  public void testText() throws MolgenisException {
    executeTest(TEXT, "This is a hello world");
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(UUID, "f83133cc-aeaa-11e9-a2a3-2a2ae2dbcce4");
  }

  private void executeTest(Column.Type type, Object value) throws MolgenisException {

    Database db = DatabaseFactory.getDatabase();

    Schema s = db.createSchema("TestRefs" + type.toString().toUpperCase());

    Table a = s.createTable("A");
    String fieldName = type + "Col";
    a.addColumn(fieldName, type);
    a.addUnique(fieldName);
    a.insert(new Row().set(fieldName, value));

    Table b = s.createTable("B");
    String refName = type + "Ref";
    b.addRef(refName, "A", fieldName);
    b.insert(new Row().set(refName, value));
  }
}
