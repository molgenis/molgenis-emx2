package org.molgenis;

import org.junit.Test;
import org.molgenis.beans.Mapper;
import org.molgenis.data.Row;
import org.molgenis.metadata.ColumnMetadata;
import org.molgenis.metadata.TableMetadata;
import org.molgenis.metadata.Type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.molgenis.metadata.Type.*;

public class CanMapPojoToTable {

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
    ColumnMetadata molgenisid = t.getColumn("molgenisid");

    ColumnMetadata firstName = t.getColumn("firstName");
    ColumnMetadata lastName = t.getColumn("lastName");

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
