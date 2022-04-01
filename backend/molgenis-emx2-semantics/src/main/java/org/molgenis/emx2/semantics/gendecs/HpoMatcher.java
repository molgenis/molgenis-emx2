package org.molgenis.emx2.semantics.gendecs;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

  public Variants getHpoMatches() {
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

  private Variants matchVariants() throws IOException {
    Variants variants = new Variants();
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
        matchHpoObjects(variants, writer, currentLine, splittedLine, hpoTermsToMatch);
      } else {
        for (String hpoTerm : hpoTermsToMatch) {
          String currentTerm = hpoTerm.trim();
          if (matchHpoTerms(currentTerm, hpoTerms.get(0))) {
            addMatchedVariant(hpoTerm, splittedLine, writer, currentLine, variants);
          }
        }
      }
    }
    reader.close();
    writer.close();
    return variants;
  }

  private void matchHpoObjects(
      Variants variants,
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
      for (String hpoTerm : matchedTerms) {
        addMatchedVariant(hpoTerm, splittedLine, writer, currentLine, variants);
      }
    }
  }

  private void addMatchedVariant(
      String hpoTerm,
      String[] splittedLine,
      BufferedWriter writer,
      String currentLine,
      Variants variants)
      throws IOException {
    logger.debug(hpoTerm + "has match with entered HPO term(s)" + hpoTerms);
    String gene = splittedLine[7].split("\\|")[3];
    writer.write(currentLine + System.getProperty("line.separator"));
    variants.addVariant(currentLine);
    variants.addGenesHpo(gene, hpoTerm.trim());
  }

  private String[] getTermsToMatch(String[] splittedLine) {
    String hpoTermToMatch = splittedLine[splittedLine.length - 1].replace("[", "");
    hpoTermToMatch = hpoTermToMatch.replace("]", "");
    logger.debug("Current hpoTerm: " + hpoTermToMatch);
    return hpoTermToMatch.split(",");
  }

  private boolean matchHpoTerms(String hpoTerm, HpoTerm hpoTermObject) throws IOException {
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
