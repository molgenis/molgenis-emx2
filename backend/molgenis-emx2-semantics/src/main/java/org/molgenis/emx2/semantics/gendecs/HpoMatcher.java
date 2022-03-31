package org.molgenis.emx2.semantics.gendecs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class HpoMatcher {
  private static final Logger logger = LoggerFactory.getLogger(HpoMatcher.class);
  private ArrayList<String> hpoTerms;
  private String filteredClinvar;
  private final String pathName = "data/gendecs/result_matches.vcf";

  public HpoMatcher(ArrayList<String> hpoTerms, String filteredClinvar) {
    this.hpoTerms = hpoTerms;
    this.filteredClinvar = filteredClinvar;
  }

  public Variants getHpoMatches() {
    try {
      Path path = Paths.get(pathName);
      if (!path.toFile().isFile()) {
        ClinvarMatcher clinvarMatcher = new ClinvarMatcher(hpoTerms, filteredClinvar);
        clinvarMatcher.matchWithClinvar();
      }
      return this.matchHpo();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private Variants matchHpo() throws IOException {
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
      String hpoTerm = splittedLine[splittedLine.length - 1];
      logger.debug("Current hpoTerm: " + hpoTerm);
      if (hpoTerms.contains(hpoTerm)) {
        logger.debug(hpoTerm + "has match with entered HPO term(s)" + hpoTerms);
        String gene = splittedLine[7].split("\\|")[3];
        writer.write(currentLine + System.getProperty("line.separator"));
        variants.addVariant(currentLine);
        variants.addGenesHpo(gene, hpoTerm);
      }
    }
    reader.close();
    writer.close();
    return variants;
  }
}
