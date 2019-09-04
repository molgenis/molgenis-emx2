package org.molgenis.emx2;

import org.junit.Test;
import org.molgenis.emx2.beans.Mapper;
import org.molgenis.emx2.beans.PersonBean;
import org.molgenis.emx2.beans.TypeTestBean;
import org.molgenis.emx2.utils.MolgenisException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.molgenis.emx2.Type.*;

public class TestMapPojoToTable {

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
  public void testPersonClassToTable() throws MolgenisException {
    TableMetadata t = Mapper.map(PersonBean.class);
    Column molgenisid = t.getColumn("molgenisid");

    Column firstName = t.getColumn("firstName");
    Column lastName = t.getColumn("lastName");

    assertEquals("firstName", firstName.getColumnName());
    assertEquals("lastName", lastName.getColumnName());

    assertEquals(Type.UUID, molgenisid.getType());
    assertEquals(STRING, firstName.getType());

    assertTrue(firstName.getNullable());
    assertFalse(lastName.getNullable());

    assertEquals("This is optional first name", firstName.getDescription());
  }

  @Test
  public void testTypeTestToTable() throws MolgenisException {
    TableMetadata table = Mapper.map(TypeTestBean.class);

    for (Type type : new Type[] {STRING, INT, DECIMAL, BOOL, DATE, DATETIME}) {

      String columnName =
          "a"
              + type.toString().substring(0, 1).toUpperCase()
              + type.toString().substring(1).toLowerCase();

      assertEquals(type, table.getColumn(columnName).getType());
    }
  }
}
