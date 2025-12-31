package org.molgenis.emx2.typescript;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import org.junit.jupiter.api.Test;

class AToolToGenerateTypeScriptTypesTest {

  @Test
  void runWithMissingArgs() {
    PrintStream old = System.out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(baos);
    System.setOut(out);

    String[] args = new String[] {};
    try {
      AToolToGenerateTypeScriptTypes.main(args);
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.flush();
    System.setOut(old);
    String s = new String(baos.toByteArray(), Charset.defaultCharset());
    assertEquals(
        "Generating TypeScript types\n"
            + "Missing required arguments ( SchemaName ( example: 'Pet Store'), full-file-path (example: '/home/bob/app/types.ts')\n",
        s);
  }

  @Test
  void runWithSingleArg() {
    PrintStream old = System.out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(baos);
    System.setOut(out);

    String[] args = new String[] {"pet store"};
    try {
      AToolToGenerateTypeScriptTypes.main(args);
    } catch (Exception e) {
      e.printStackTrace();
    }

    System.out.flush();
    System.setOut(old);
    String s = new String(baos.toByteArray(), Charset.defaultCharset());
    assertEquals(
        "Generating TypeScript types\n"
            + "Missing required arguments ( SchemaName ( example: 'Pet Store'), full-file-path (example: '/home/bob/app/types.ts')\n",
        s);
  }

  @Test
  void runWithValidArgs() {
    PrintStream old = System.out;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream out = new PrintStream(baos);
    System.setOut(out);

    String[] args = new String[] {"pet store", "'/home/bob/app/types.ts"};
    try {
      AToolToGenerateTypeScriptTypes.main(args);
    } catch (Exception e) {
      System.out.flush();
      System.setOut(old);
      String s = new String(baos.toByteArray(), Charset.defaultCharset());

      assertTrue(
          s.contains(
              "Generating TypeScript types\n"
                  + "  Generate from Schema: pet store\n"
                  + "  Generate into file: '/home/bob/app/types.ts"));
    }
  }
}
