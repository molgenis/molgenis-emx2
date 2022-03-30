package org.molgenis.emx2.semantics.gendecs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClinvarFilter {
  StarRating starRating;
  private static final Logger logger = LoggerFactory.getLogger(ClinvarFilter.class);

  public ClinvarFilter(StarRating starRating) {
    this.starRating = starRating;
  }

  private ArrayList<String> getRating(StarRating starRating) {
    switch (starRating) {
      case ZEROSTAR -> {
        return new ArrayList<>(List.of("no_assertion_provided", "no_assertion_criteria_provided"));
      }
      case ONESTAR -> {
        return new ArrayList<>(
            List.of(
                "criteria_provided,_single_submitter",
                "criteria_provided,_conflicting_interpretations",
                "no_assertion_provided",
                "no_assertion_criteria_provided"));
      }
      case TWOSTAR -> {
        return new ArrayList<>(
            List.of(
                "criteria_provided,_multiple_submitters,_no_conflicts",
                "criteria_provided,_single_submitter",
                "criteria_provided,_conflicting_interpretations",
                "no_assertion_provided",
                "no_assertion_criteria_provided"));
      }
      case THREESTAR -> {
        return new ArrayList<>(
            List.of(
                "reviewed_by_expert_panel",
                "criteria_provided,_multiple_submitters,_no_conflicts",
                "criteria_provided,_single_submitter",
                "criteria_provided,_conflicting_interpretations",
                "no_assertion_provided",
                "no_assertion_criteria_provided"));
      }
    }
    return null;
  }

  /**
   * Method removeStatus removes given starRating from input clinvar file.
   *
   * @param inputFile clinvar file as vcf
   * @return boolean true if successful, false if failed
   */
  public String removeStatus(String inputFile) {
    String pathName = String.format("data/gendecs/Filtered_Clinvar_%s.vcf", this.starRating);
    Path path = Paths.get(pathName);
    if (path.toFile().isFile()) {
      return pathName;
    } else {
      File filteredClinVar = new File(pathName);
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filteredClinVar));
        File fileObject = new File(inputFile);
        Scanner reader = new Scanner(fileObject);
        while (reader.hasNextLine()) {
          String data = reader.nextLine();
          if (stringContainsItemFromList(
              data, Objects.requireNonNull(getRating(this.starRating)))) {
            continue;
          }
          writer.write(data + System.getProperty("line.separator"));
        }
        reader.close();
        writer.close();
        return pathName;
      } catch (IOException e) {
        e.printStackTrace();
      }
      return "";
    }
  }

  private static boolean stringContainsItemFromList(String inputString, ArrayList<String> items) {
    return items.stream().anyMatch(inputString::contains);
  }
}
