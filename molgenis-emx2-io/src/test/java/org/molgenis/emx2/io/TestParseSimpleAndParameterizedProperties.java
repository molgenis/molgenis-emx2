package org.molgenis.emx2.io;

import org.junit.Test;
import org.molgenis.emx2.io.emx2.Emx2PropertyList;
import org.molgenis.emx2.utils.MolgenisExceptionDetail;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TestParseSimpleAndParameterizedProperties {

  @Test
  public void test() {

    // string ref(other table) function(if\\(true\\){ return 1; } else { return false })
    // function2(blaat) test
    // String[] tests = new String[]{"int good(test)","string ref(other table)
    // function(if\\(true\\){ return 1; } else { return false }) functions(blaat) test"};
    String[] tests =
        new String[] {
          "int ref(test)",
          "string ref(other table) check(if\\(true\\){ return 1; } else { return false }) nillable blaat readonly(blaat)"
        };

    int line = 1;
    for (String testString : tests) {
      System.out.println("testing definition string: '" + testString + "'");
      List<MolgenisExceptionDetail> messages = new ArrayList<>();

      Emx2PropertyList def = new Emx2PropertyList(testString);
      // both have a ref
      assertTrue(def.contains("ref"));

      for (String tag : def.getTerms()) {
        System.out.println(tag + " with parameter" + def.getParamterValue(tag));
      }
      System.out.println("messages:");
      for (MolgenisExceptionDetail message : messages) {
        System.out.println(message);
      }
      System.out.println("\n");
    }
  }
}
