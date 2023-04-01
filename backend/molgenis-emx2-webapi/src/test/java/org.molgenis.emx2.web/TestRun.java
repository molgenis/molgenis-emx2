package org.molgenis.emx2.web;

import org.junit.Test;
import org.molgenis.emx2.Constants;
import org.molgenis.emx2.RunMolgenisEmx2;

public class TestRun {

  @Test
  public void testRun() {
    RunMolgenisEmx2.main(new String[] {"-D" + Constants.MOLGENIS_HTTP_PORT, "8080"});
  }
}
