package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.MolgenisException;
import org.molgenis.Schema;
import org.molgenis.beans.SchemaBean;
import org.molgenis.utils.StopWatch;

import java.io.File;
import java.io.IOException;

public class LoaderTest {

  @Test
  public void loadTest1() {
    StopWatch.start("loadTest1");
    Schema s = new SchemaBean("test");
    try {
      ZipLoader.load(s, new File(ClassLoader.getSystemResource("molgenis1").getFile()));
    } catch (MolgenisException me) {
      me.printMessages();
    } catch (IOException e) {
      e.printStackTrace();
    }
    StopWatch.print("schema loaded");
    System.out.println(s);
  }
}
