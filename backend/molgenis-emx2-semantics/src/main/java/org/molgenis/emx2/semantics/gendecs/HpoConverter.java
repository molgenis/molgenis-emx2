package org.molgenis.emx2.semantics.gendecs;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

public class HpoConverter {

  public static String getHpoTerm(String id, String genes_to_pheno) {
    try {
      Scanner reader = new Scanner(new File(genes_to_pheno));
      while (reader.hasNextLine()) {
        String currenLine = reader.nextLine();
        if (currenLine.contains(id)) {
          String[] lineSplit = currenLine.split("\t");
          reader.close();
          return lineSplit[3];
        }
      }
      reader.close();
      return "";
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  public static String getHpoId(String hpoTerm, String genes_to_pheno) {
    try {
      Scanner reader = new Scanner(new File(genes_to_pheno));
      while (reader.hasNextLine()) {
        String currenLine = reader.nextLine();
        if (currenLine.contains(hpoTerm)) {
          String[] lineSplit = currenLine.split("\t");
          reader.close();
          return lineSplit[2];
        }
      }
      reader.close();
      return "";
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }
}
