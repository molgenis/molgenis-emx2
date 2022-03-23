package org.molgenis.emx2.semantics.gendecs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class VariantHpoMatcher {
  static HashMap<String, String> geneHpo = new HashMap<>();
  ArrayList<String> hpoTerms;

  public VariantHpoMatcher(ArrayList<String> hpoTerms) {
    this.hpoTerms = hpoTerms;
  }

  public boolean matchVariantWithHpo(String variant) {
    String geneSymbol = getGenes(variant);
    if (this.getHpo(geneSymbol)) {
      return true;
    }

    return false;
  }

  private static String getGeneSymbol(String geneRaw) {
    String[] geneSplit = geneRaw.split(":");
    if (geneSplit.length > 2) {
      // TODO filter the genes with duplicates example below:
      // [TTC21B, 79809|TTC21B-AS1, 100506134]
      //                [HBB, 3043|LOC106099062, 106099062|LOC107133510, 107133510]
      //                geneSymbols.add(geneSplit[0]);
      return "";
    } else {
      return geneSplit[0];
    }
  }

  private static String getGenes(String variant) {
    String[] splittedLine = variant.split("\t");
    String[] infoString = splittedLine[7].split(";");
    for (String i : infoString) {
      if (i.contains("GENEINFO")) {
        return (getGeneSymbol(i.split("=")[1]));
      }
    }
    return null;
  }

  private boolean getHpo(String geneSymbol) {
    try {
      File file = new File("data/gendecs/genes_to_phenotype.txt");
      Scanner reader = new Scanner(file);
      while (reader.hasNextLine()) {
        String currentLine = reader.nextLine();
        if (currentLine.contains(geneSymbol)) {
          String[] lineSplit = reader.nextLine().split("\t");
          String gene = lineSplit[1];
          String hpoId = lineSplit[2];
          String hpoTerm = lineSplit[3];
          geneHpo.put(gene, hpoTerm);
          if (this.checkForMatch(hpoTerm)) {
            return true;
          }
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return false;
  }

  private boolean checkForMatch(String hpoTerm) {
    //        HashMap<String, String> matchedGenes = new HashMap<>();

    for (String hpoTermIn : this.hpoTerms) {
      if (hpoTermIn.equals(hpoTerm)) {
        return true;
      }
    }
    return false;
  }

  public static HashMap<String, String> getGeneHpo() {
    return geneHpo;
  }
}
