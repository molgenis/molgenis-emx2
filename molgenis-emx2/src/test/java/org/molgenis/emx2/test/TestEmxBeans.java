package org.molgenis.emx2.test;

import org.junit.Test;
import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.molgenis.emx2.EmxType.*;

public class TestEmxBeans {

  @Test
  public void test1() throws EmxException {
    List<EmxType> types = Arrays.asList(STRING, INT, DECIMAL, BOOL, UUID, TEXT, DATE, DATETIME);

    EmxModel m = new EmxModel();
    addContents(m, types);

    EmxModel m2 = new EmxModel();
    addContents(m2, types);

    System.out.println("No diff: " + m.diff(m2));

    assertNotNull(m.getTableNames().contains("TypeTest"));
    assertEquals(1, m.getTables().size());

    System.out.println("model print: " + m.print());
    EmxTable t = m.getTable("TypeTest");
    assertEquals("TypeTest", t.getName());
    assertEquals(3 * types.size(), t.getColumns().size());
    assertEquals(EmxType.BOOL, t.getColumn("testBOOL").getType());

    System.out.println("table print " + t.toString() + "\n: " + t.print());

    m2.addTable("OtherTable");
    System.out.println("Now we expect diff: " + m.diff(m2));

    m.removeTable("TypeTest");
    assertNull(m.getTable("TypeTest"));
    assertEquals(0, m.getTables().size());
  }

  private void addContents(EmxModel m, List<EmxType> types) throws EmxException {
    EmxTable t = m.addTable("TypeTest");
    for (EmxType type : types) {
      t.addColumn("test" + type, type);
      t.addColumn("test" + type + "_nullable", type).setNillable(true);
      t.addColumn("test" + type + "+readonly", type).setReadonly(true);
    }
  }
}
