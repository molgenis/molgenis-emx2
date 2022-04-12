package org.molgenis.emx2.semantics.gendecs;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HpoMatcher {
  private static final Logger logger = LoggerFactory.getLogger(HpoMatcher.class);
  private final ArrayList<HpoTerm> hpoTerms;
  private final String filteredClinvar;
  private final String pathName = "data/gendecs/result_matches.vcf";

  public HpoMatcher(ArrayList<HpoTerm> hpoTerms, String filteredClinvar) {
    this.hpoTerms = hpoTerms;
    this.filteredClinvar = filteredClinvar;
  }

  public ArrayList<Variant> getHpoMatches() {
    try {
      Path path = Paths.get(pathName);
      if (!path.toFile().isFile()) {
        ClinvarMatcher clinvarMatcher = new ClinvarMatcher(filteredClinvar);
        clinvarMatcher.matchWithClinvar();
      }
      return this.matchVariants();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private ArrayList<Variant> matchVariants() throws IOException {
    ArrayList<Variant> variantList = new ArrayList<>();
    logger.info("Matching the HPO terms");
    File matchedLinesFile = new File("data/gendecs/result_hpo_matches.vcf");
    BufferedWriter writer = new BufferedWriter(new FileWriter(matchedLinesFile));
    VcfFile.writeHeader(writer, "result");
    File file = new File(pathName);
    Scanner reader = new Scanner(file);

    while (reader.hasNextLine()) {
      String currentLine = reader.nextLine();
      if (currentLine.startsWith("#")) {
        continue;
      }
      String[] splittedLine = currentLine.split("\t");
      String[] hpoTermsToMatch = getTermsToMatch(splittedLine);

      if (hpoTerms.size() > 1) {
        matchHpoObjects(variantList, writer, currentLine, splittedLine, hpoTermsToMatch);
      } else {
        for (int i = 0; i < hpoTermsToMatch.length; i++) {
          String currentTerm = hpoTermsToMatch[i].trim();
          if (matchHpoTerms(currentTerm, hpoTerms.get(0))) {
            addMatchedVariant(
                hpoTermsToMatch[i].trim(), splittedLine, writer, currentLine, variantList, i);
          }
        }
      }
    }
    reader.close();
    writer.close();
    return variantList;
  }

  private void matchHpoObjects(
      ArrayList<Variant> variantList,
      BufferedWriter writer,
      String currentLine,
      String[] splittedLine,
      String[] hpoTermsToMatch)
      throws IOException {
    ArrayList<String> matchedTerms = new ArrayList<>();
    int itemsToMatch = hpoTerms.size();
    int itemsMatched = 0;
    for (HpoTerm hpoTermObject : hpoTerms) {
      for (String hpoTerm : hpoTermsToMatch) {
        String currentTerm = hpoTerm.trim();

        if (matchHpoTerms(currentTerm, hpoTermObject)) {
          matchedTerms.add(currentTerm);
          itemsMatched++;
        }
      }
    }
    if (itemsMatched == itemsToMatch) {
      for (int i = 0; i < matchedTerms.size(); i++) {
        addMatchedVariant(matchedTerms.get(i), splittedLine, writer, currentLine, variantList, i);
      }
    }
  }

  private void addMatchedVariant(
      String hpoTerm,
      String[] splittedLine,
      BufferedWriter writer,
      String currentLine,
      ArrayList<Variant> variantList,
      int i)
      throws IOException {
    Variant variant = new Variant();
    logger.debug(hpoTerm + "has match with entered HPO term(s)" + hpoTerms);
    String gene = splittedLine[7].split("\\|")[3];
    String diseaseId = splittedLine[splittedLine.length - 1].split(",")[i];
    writer.write(currentLine + System.getProperty("line.separator"));

    String[] variantLine = currentLine.split("\t");
    String variantString =
        Arrays.toString(Arrays.copyOfRange(variantLine, 0, variantLine.length - 2));
    variant.setVariant(variantString.trim());
    variant.setGene(gene.trim());
    variant.setDisease(diseaseId.trim());
    variant.setHpoTerm(hpoTerm.trim());
    variantList.add(variant);
  }

  private String[] getTermsToMatch(String[] splittedLine) {
    String hpoTermToMatch = splittedLine[splittedLine.length - 2].replace("[", "");
    hpoTermToMatch = hpoTermToMatch.replace("]", "");
    logger.debug("Current hpoTerm: " + hpoTermToMatch);
    return hpoTermToMatch.split(",");
  }

  private boolean matchHpoTerms(String hpoTerm, HpoTerm hpoTermObject) {
    String currentTerm = hpoTerm.trim();
    ArrayList<String> children = new ArrayList<>();
    ArrayList<String> parents = new ArrayList<>();
    if (hpoTermObject.getChildren() != null) {
      children = hpoTermObject.getChildren();
    }
    if (hpoTermObject.getParents() != null) {
      parents = hpoTermObject.getParents();
    }
    return hpoTermObject.getHpoTerm().equals(currentTerm)
        || children.contains(currentTerm)
        || parents.contains(currentTerm);
  }
}
