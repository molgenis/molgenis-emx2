package org.molgenis;

import org.junit.Test;
import org.molgenis.beans.ColumnBean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMapper {

  @Test
  public void testBeanToRowToBean() throws MolgenisException {
    PersonBean b = new PersonBean();
    b.setFirstName("Donald");
    b.setLastName("Duck");

    Row[] rows = Mapper.map(b);

    assertEquals(1, rows.length);

    Row r = rows[0];
    assertEquals(r.getString("firstName"), b.getFirstName());
    assertEquals(r.getString("lastName"), b.getLastName());

    PersonBean b2 = Mapper.map(PersonBean.class, r);

    assertEquals(b.getFirstName(), b2.getFirstName());
    assertEquals(b.getLastName(), b2.getLastName());
  }

  @Test
  public void testClassToTable() throws MolgenisException {
    Table t = Mapper.map(PersonBean.class);
    Column molgenisid = t.getColumn("molgenisid");

    Column firstName = t.getColumn("firstName");
    Column lastName = t.getColumn("lastName");

    assertEquals(firstName.getName(), "firstName");
    assertEquals(lastName.getName(), "lastName");

    assertEquals(molgenisid.getType(), Column.Type.UUID);
    assertEquals(firstName.getType(), Column.Type.STRING);

    assertTrue(firstName.isNullable());
    assertFalse(lastName.isNullable());

    assertEquals(firstName.getDescription(), "This is optional first name");
  }

  @Test
  public void testColumnBeanToRow() throws MolgenisException {
    Table t = Mapper.map(ColumnBean.class);
    System.out.println(t);
  }
}
