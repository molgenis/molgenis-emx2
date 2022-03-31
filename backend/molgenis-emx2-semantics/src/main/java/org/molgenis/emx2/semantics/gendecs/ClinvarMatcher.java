package org.molgenis.emx2.semantics.gendecs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClinvarMatcher {
  private File vcfFile;
  private ArrayList<String> hpoTerms;
  private File clinvarFile;

  private static final Logger logger = LoggerFactory.getLogger(ClinvarMatcher.class);

  public ClinvarMatcher(ArrayList<String> hpoTerms, String clinvarLocation) {
    vcfFile = new File(Constants.FILENAMEVCFDATA);
    this.hpoTerms = hpoTerms;
    clinvarFile = new File(clinvarLocation);
  }

  /**
   * Method matchWithClinvar reads given vcf file with variants and builds regex for each line.
   * Which is then used to match this with variants in the clinvar vcf file. The found matches are
   * filtered on the variant being pathogenic or not. The remaining variants are added to the
   * Variants class.
   */
  public void matchWithClinvar() {
    Map<String, Pattern> stringsToFind = new HashMap<>();
    try {
      Scanner reader = new Scanner(vcfFile);
      while (reader.hasNextLine()) {
        String data = reader.nextLine();
        if (data.startsWith("#")) {
          continue;
        }
        String[] splittedData = data.split("\t");
        String chromosome = splittedData[0];
        String position = splittedData[1];
        String ref = splittedData[3];
        String alt = splittedData[4];
        Pattern pattern =
            Pattern.compile(
                String.format("%s\\t%s\\t[0-9]+\\t%s\\t%s.+", chromosome, position, ref, alt));
        stringsToFind.put(data, pattern);
      }
      reader.close();

      getMatchesClinvar(stringsToFind);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void getMatchesClinvar(Map<String, Pattern> stringsToFind) throws IOException {
    Scanner reader = new Scanner(clinvarFile);

    File resultFileClinvar = new File("data/gendecs/result_matches_clinvar.vcf");
    File resultFileResult = new File("data/gendecs/result_matches.vcf");
    BufferedWriter writerResult = new BufferedWriter(new FileWriter(resultFileResult));
    BufferedWriter writerClinvar = new BufferedWriter(new FileWriter(resultFileClinvar));

    VcfFile.writeHeader(writerClinvar, "clinvar");
    VcfFile.writeHeader(writerResult, "result");
    VariantHpoMatcher variantHpoMatcher = new VariantHpoMatcher();

    while (reader.hasNextLine()) {
      String currentLine = reader.nextLine();
      for (Pattern stringToFind : stringsToFind.values()) {
        if (currentLine.matches(String.valueOf(stringToFind))) {
          if (isPathogenic(currentLine)) {
            logger.debug("The following line is pathogenic: " + currentLine);
            ArrayList<String> hpoTerms = variantHpoMatcher.matchVariantWithHpo(currentLine);

            writerResult.write(
                getKeyFromValue(stringsToFind, stringToFind)
                    + '\t'
                    + hpoTerms
                    + System.getProperty("line.separator"));
            writerClinvar.write(
                currentLine + '\t' + hpoTerms + System.getProperty("line.separator"));
          }
        }
      }
    }
    reader.close();
    writerClinvar.close();
    writerResult.close();
  }

  private static String getKeyFromValue(Map<String, Pattern> map, Pattern value) {
    return map.entrySet().stream()
        .filter(entry -> Objects.equals(entry.getValue(), value))
        .map(Map.Entry::getKey)
        .findFirst()
        .map(Object::toString)
        .orElse("");
  }

  private static boolean isPathogenic(String variant) {
    ArrayList<String> clinSig =
        new ArrayList<>(List.of("likely_pathogenic", "pathogenic", "pathogenic/likely_pathogenic"));
    String[] splittedLine = variant.split("\t");
    String[] infoString = splittedLine[7].split(";");

    for (String i : infoString) {
      if (i.contains("CLNSIG")) {
        if (clinSig.contains(i.split("=")[1].toLowerCase())) {
          return true;
        }
      }
    }
    return false;
  }
}
