package org.molgenis.emx2.semantics.gendecs;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Class HpoConverter. Has two methods which transforms a given HPO term to the id or the other way
 * from id to HPO term.
 */
public class HpoConverter {

  /**
   * Method gets a HPO id and converts this to the HPO term. It reads the file
   * genes_to_phenotype.txt and searches for the given id to find the corresponding term.
   *
   * @param id of a HPO term
   * @param genesToPheno location of genes_to_phenotype.txt
   * @return String HPO term
   */
  public static String getHpoTerm(String id, String genesToPheno) {
    if (id != null) {
      try {
        Scanner reader = new Scanner(new File(genesToPheno));
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
    }
    return "";
  }

  /**
   * Method gets a HPO term and converts this to the HPO id. It reads the file
   * genes_to_phenotype.txt and searches for the given term to find the corresponding id.
   *
   * @param hpoTerm String with the HPO term
   * @param genesToPheno location of genes_to_phenotype.txt
   * @return String with hpo id
   */
  public static String getHpoId(String hpoTerm, String genesToPheno) {
    if (hpoTerm != null) {
      try {
        Scanner reader = new Scanner(new File(genesToPheno));
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
    }
    return "";
  }
}
