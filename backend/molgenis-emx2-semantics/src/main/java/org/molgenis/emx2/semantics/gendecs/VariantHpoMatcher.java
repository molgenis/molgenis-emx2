package org.molgenis.emx2.semantics.gendecs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class VariantHpoMatcher {

  public ArrayList<String> matchVariantWithHpo(String variant) {
    String geneSymbol = getGenes(variant);
    return this.getHpo(geneSymbol);
  }

  private static String getGeneSymbol(String geneRaw) {
    String[] geneSplit = geneRaw.split(":");
    if (geneSplit.length > 2) {
      // TODO filter the genes with duplicates example below:
      // [TTC21B, 79809|TTC21B-AS1, 100506134]
      //                [HBB, 3043|LOC106099062, 106099062|LOC107133510, 107133510]
      //                geneSymbols.add(geneSplit[0]);
      //      PINK1:65018|PINK1-AS:100861548
      return geneSplit[0];
    } else {
      return geneSplit[0];
    }
  }

  private static String getGenes(String variant) {
    String[] splittedLine = variant.split("\t");
    String[] infoString = splittedLine[7].split(";");
    //    System.out.println("infoString" + Arrays.toString(infoString));
    for (String i : infoString) {
      if (i.contains("GENEINFO")) {
        return (getGeneSymbol(i.split("=")[1]));
      }
    }
    return null;
  }

  private ArrayList<String> getHpo(String geneSymbol) {
    ArrayList<String> hpoTerms = new ArrayList<>();
    try {
      File file = new File("data/gendecs/genes_to_phenotype.txt");
      Scanner reader = new Scanner(file);
      while (reader.hasNextLine()) {
        String currentLine = reader.nextLine();
        if (currentLine.contains(geneSymbol)) {
          String[] lineSplit = currentLine.split("\t");
          String gene = lineSplit[1];
          String hpoId = lineSplit[2];
          String hpoTerm = lineSplit[3];
          String diseaseId = lineSplit[8];
          hpoTerms.add(hpoTerm);
        }
      }
      return hpoTerms;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return hpoTerms;
  }
}
