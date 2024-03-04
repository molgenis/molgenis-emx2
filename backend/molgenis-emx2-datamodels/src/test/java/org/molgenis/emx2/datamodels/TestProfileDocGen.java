package org.molgenis.emx2.datamodels;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.datamodels.profiles.ProfileDocGen;

class TestProfileDocGen {

  private static final String TESTFILE = "testdoc.md";

  @Test
  void testProfileDocGen() throws Exception {
    new ProfileDocGen(TESTFILE).makeDocs();
    File testFile = new File(TESTFILE);
    StringBuilder fileContentsSB = new StringBuilder();
    Scanner s = new Scanner(testFile);
    while (s.hasNextLine()) {
      fileContentsSB.append(s.nextLine());
    }
    String fileContents = fileContentsSB.toString();
    // headers
    assertTrue(fileContents.contains("EMX2"));
    assertTrue(fileContents.contains("profile"));
    // tables
    assertTrue(fileContents.contains("Cohorts"));
    assertTrue(fileContents.contains("Individuals"));
    // columns
    assertTrue(fileContents.contains("ConsentFormUsed"));
    assertTrue(fileContents.contains("Phenotype"));
    assertTrue(fileContents.contains("PathologicalState"));
    // column semantics
    assertTrue(fileContents.contains("http://purl.obolibrary.org/obo/GSSO_011434"));
    // table semantics
    assertTrue(fileContents.contains("http://purl.obolibrary.org/obo/NCIT_C17248"));
    testFile.delete();
  }
}
