package org.molgenis.emx.reader.test;

import org.junit.Test;
import org.molgenis.emx2.io.MolgenisReaderMessage;
import org.molgenis.emx2.io.format.EmxDefinitionParser;
import org.molgenis.emx2.io.format.EmxDefinitionTerm;

import java.util.ArrayList;
import java.util.List;

public class TestDefinitionParser {

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
    for (String t : tests) {
      System.out.println("testing definition string: '" + t + "'");
      List<MolgenisReaderMessage> messages = new ArrayList<>();
      for (EmxDefinitionTerm tag : new EmxDefinitionParser().parse(line++, messages, t)) {
        System.out.println(tag);
      }
      System.out.println("messages:");
      for (MolgenisReaderMessage message : messages) {
        System.out.println(message);
      }
      System.out.println("\n");
    }
  }
}
