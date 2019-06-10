package org.molgenis.emx2.test;

import org.junit.Test;
import org.molgenis.Column;
import org.molgenis.DatabaseException;
import org.molgenis.Schema;
import org.molgenis.Table;
import org.molgenis.bean.SchemaBean;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.molgenis.Column.Type.*;

public class TestBeans {

  @Test
  public void test1() throws DatabaseException {
    List<Column.Type> types = Arrays.asList(STRING, INT, DECIMAL, BOOL, UUID, TEXT, DATE, DATETIME);

    Schema m = new SchemaBean();
    addContents(m, types);

    Schema m2 = new SchemaBean();
    addContents(m2, types);

    // System.out.println("No diff: " + m.diff(m2));

    assertNotNull(m.getTables().contains("TypeTest"));
    assertEquals(1, m.getTables().size());

    // System.out.println("model print: " + m.print());
    Table t = m.getTable("TypeTest");
    assertEquals("TypeTest", t.getName());
    assertEquals(3 * types.size(), t.getColumns().size());
    assertEquals(BOOL, t.getColumn("testBOOL").getType());

    // System.out.println("table print " + t.toString() + "\n: " + t.print());

    m2.createTable("OtherTable");
    // System.out.println("Now we expect diff: " + m.diff(m2));

    m.dropTable("TypeTest");
    assertNull(m.getTable("TypeTest"));
    assertEquals(0, m.getTables().size());
  }

  private void addContents(Schema m, List<Column.Type> types) throws DatabaseException {
    Table t = m.createTable("TypeTest");
    for (Column.Type type : types) {
      t.addColumn("test" + type, type);
      t.addColumn("test" + type + "_nullable", type).setNullable(true);
      t.addColumn("test" + type + "+readonly", type).setReadonly(true);
    }
  }
}
