package org.molgenis.sql;

import org.junit.Test;
import org.molgenis.*;

import static org.molgenis.Column.Type.*;

public class TestRefs {

  @Test
  public void testInt() throws MolgenisException {
    executeTest(INT);
  }

  @Test
  public void testString() throws MolgenisException {
    executeTest(STRING);
  }

  @Test
  public void testDate() throws MolgenisException {
    executeTest(DATE);
  }

  @Test
  public void testDateTime() throws MolgenisException {
    executeTest(DATETIME);
  }

  @Test
  public void testDecimal() throws MolgenisException {
    executeTest(DECIMAL);
  }

  @Test
  public void testText() throws MolgenisException {
    executeTest(TEXT);
  }

  @Test
  public void testUUID() throws MolgenisException {
    executeTest(UUID);
  }

  private void executeTest(Column.Type type) throws MolgenisException {

    Database db = DatabaseFactory.getDatabase();

    Schema s = db.createSchema("TestRefs" + type.toString().toUpperCase());

    Table a = s.createTable("A");
    String fieldName = type + "Col";
    a.addColumn(fieldName, type);
    a.addUnique(fieldName);

    Table b = s.createTable("B");
    String refName = type + "Ref";
    b.addRef(refName, "A", fieldName);
  }
}
