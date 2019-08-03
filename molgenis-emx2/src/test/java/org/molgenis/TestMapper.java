package org.molgenis;

import org.junit.Test;
import org.molgenis.beans.Mapper;

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
    assertEquals(r.getString("FirstName"), b.getFirstName());
    assertEquals(r.getString("LastName"), b.getLastName());
    System.out.println(r);

    PersonBean b2 = Mapper.map(PersonBean.class, r);
    System.out.println(b2);

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

    assertEquals(molgenisid.getType(), Type.UUID);
    assertEquals(firstName.getType(), Type.STRING);

    assertTrue(firstName.isNullable());
    assertFalse(lastName.isNullable());

    assertEquals(firstName.getDescription(), "This is optional first name");
  }
}
