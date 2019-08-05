package org.molgenis;

import org.junit.Test;
import org.molgenis.beans.SchemaBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;
import static org.molgenis.Type.*;

public class TestBeans {

  @Test
  public void test1() throws MolgenisException {
    List<Type> types = Arrays.asList(STRING, INT, DECIMAL, BOOL, UUID, TEXT, DATE, DATETIME);

    Schema m = new SchemaBean("test1");
    addContents(m, types);

    Schema m2 = new SchemaBean("test1");
    addContents(m2, types);

    // System.out.println("No diff: " + m.diff(m2));

    assertNotNull(m.getTableNames().contains("TypeTest"));
    assertEquals(1, m.getTableNames().size());

    // System.out.println("model print: " + m.print());
    Table t = m.getTable("TypeTest");
    assertEquals("TypeTest", t.getName());
    assertEquals(3 * types.size(), t.getColumns().size());
    assertEquals(BOOL, t.getColumn("testBOOL").getType());

    // System.out.println("table print " + t.toString() + "\n: " + t.print());

    m2.createTableIfNotExists("OtherTable");
    // System.out.println("Now we expect diff: " + m.diff(m2));

    m.dropTable("TypeTest");
    try {
      m.getTable("TypeTest");
      fail("Table should have been dropped");
    } catch (Exception e) {
      // this is expected
    }
    assertEquals(0, m.getTableNames().size());
  }

  @Test
  public void testTypes() {
    org.molgenis.Row r = new Row();

    // int
    r.setString("test", "1");
    assertEquals(1, (int) r.getInteger("test"));
    assertNull(r.getInteger("testnull"));
    try {
      r.setString("test", "a");
      assertEquals(1, (int) r.getInteger("test"));
      fail("shouldn't be able to get 'a' to int ");
    } catch (Exception e) {
    }

    // decimal
    r.setString("test", "1.0");
    assertEquals(1.0, (double) r.getDecimal("test"));
    assertNull(r.getDecimal("testnull"));
    try {
      r.setString("test", "a");
      r.getDecimal("test");
      fail("shouldn't be able to get 'a' to decimal ");
    } catch (Exception e) {
    }

    // bool
    r.setString("test", "true");
    assertTrue(r.getBoolean("test"));
    assertNull(r.getBoolean("testnull"));
    try {
      r.setString("test", "a");
      r.getBoolean("test");
      fail("shouldn't be able to get 'a' to boolean ");
    } catch (Exception e) {
    }

    // uuid
    java.util.UUID uuid = java.util.UUID.randomUUID();
    r.setString("test", uuid.toString());
    assertEquals(uuid, r.getUuid("test"));
    assertNull(r.getUuid("testnull"));
    try {
      r.setString("test", "a");
      r.getUuid("test");
      fail("shouldn't be able to get 'a' to uuid ");
    } catch (Exception e) {
    }

    // date
    String dateString = "2012-10-03";
    r.setString("test", dateString);
    LocalDate date = LocalDate.parse(dateString);
    assertEquals(date, r.getDate("test"));
    assertNull(r.getDate("testnull"));
    try {
      r.setString("test", "a");
      r.getDate("test");
      fail("shouldn't be able to get 'a' to date ");
    } catch (Exception e) {
    }

    // datetime
    String dateTimeString = "2012-10-03T18:00";
    r.setString("test", dateTimeString);
    LocalDateTime dateTime = LocalDateTime.parse(dateTimeString);
    assertEquals(dateTime, r.getDateTime("test"));
    assertNull(r.getDate("testnull"));
    try {
      r.setString("test", "a");
      r.getDateTime("test");
      fail("shouldn't be able to get 'a' to datetime ");
    } catch (Exception e) {
    }

    // string if not a string
    r.setInt("test", 1);
    assertEquals("1", r.getString("test"));
    assertNull(r.getString("testnull"));
  }

  private void addContents(Schema m, List<Type> types) throws MolgenisException {
    Table t = m.createTableIfNotExists("TypeTest");
    for (Type type : types) {
      t.addColumn("test" + type, type);
      t.addColumn("test" + type + "_nullable", type).setNullable(true);
      t.addColumn("test" + type + "+readonly", type).setReadonly(true);
    }
  }
}
