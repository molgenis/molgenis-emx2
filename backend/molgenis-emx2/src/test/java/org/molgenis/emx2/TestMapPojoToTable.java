package org.molgenis.emx2;

import static org.junit.jupiter.api.Assertions.*;
import static org.molgenis.emx2.ColumnType.*;

import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.beans.Mapper;
import org.molgenis.emx2.beans.PersonBean;
import org.molgenis.emx2.beans.TypeTestBean;

public class TestMapPojoToTable {

  @Test
  public void testBeanToRowToBean()
      throws InvocationTargetException, IllegalAccessException, NoSuchMethodException,
          InstantiationException {
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
  public void testPersonClassToTable() {
    TableMetadata t = Mapper.map(PersonBean.class);
    Column molgenisid = t.getColumn("molgenisid");

    Column firstName = t.getColumn("firstName");
    Column lastName = t.getColumn("lastName");

    assertEquals("firstName", firstName.getName());
    assertEquals("lastName", lastName.getName());

    assertEquals(ColumnType.UUID, molgenisid.getColumnType());
    assertEquals(STRING, firstName.getColumnType());

    assertTrue(firstName.isRequired());
    assertFalse(lastName.isRequired());
  }

  @Test
  public void testTypeTestToTable() {
    TableMetadata table = Mapper.map(TypeTestBean.class);

    for (ColumnType columnType : new ColumnType[] {STRING, INT, DECIMAL, BOOL, DATE, DATETIME}) {

      String columnName =
          "a"
              + columnType.toString().substring(0, 1).toUpperCase()
              + columnType.toString().substring(1).toLowerCase();

      assertEquals(columnType, table.getColumn(columnName).getColumnType());
    }
  }
}
