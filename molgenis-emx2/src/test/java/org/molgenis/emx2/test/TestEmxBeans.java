package org.molgenis.emx2.test;

import org.junit.Test;
import org.molgenis.emx2.EmxException;
import org.molgenis.emx2.EmxModel;
import org.molgenis.emx2.EmxTable;
import org.molgenis.emx2.EmxType;

public class TestEmxBeans {

  @Test
  public void test1() throws EmxException {
    EmxModel m = new EmxModel();
    addContents(m);

    EmxModel m2 = new EmxModel();
    addContents(m2);

    System.out.println("No diff: " + m.diff(m2));

    m2.addTable("OtherTable");
    System.out.println("Now we expect diff: " + m.diff(m2));
  }

  private void addContents(EmxModel m) throws EmxException {
    EmxTable t = m.addTable("TypeTest");
    for (EmxType type : EmxType.values()) {
      t.addColumn("test" + type, type);
    }
    for (EmxType type : EmxType.values()) {
      t.addColumn("test" + type + "_nullable", type).setNillable(true);
    }
    for (EmxType type : EmxType.values()) {
      t.addColumn("test" + type + "+readonly", type).setReadonly(true);
    }
  }
}
