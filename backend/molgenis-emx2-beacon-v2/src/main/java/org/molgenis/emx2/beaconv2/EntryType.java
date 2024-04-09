package org.molgenis.emx2.beaconv2;

import static org.molgenis.emx2.beaconv2.Concept.*;

import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;

public enum EntryType {
  INDIVIDUALS(
      "Individuals",
      "individuals",
      "Individual",
      List.of(SEX, DISEASE, PHENOTYPE, CAUSAL_GENE, AGE_THIS_YEAR, AGE_OF_ONSET, AGE_AT_DIAG)),
  BIOSAMPLES(
      "Biosamples",
      "biosamples",
      "Biosample",
      List.of(SEX, DISEASE, AGE_THIS_YEAR, AGE_AT_DIAG, BIOSPECIMIN_TYPE)),
  DATASETS("Dataset", "datasets", "Dataset", null),
  ANALYSES("Analyses", "analyses", "Analysis", null),
  COHORTS("Cohorts", "cohorts", "Cohort", null),
  RUNS("Runs", "runs", "Run", null),
  GENOMIC_VARIANT("GenomicVariations", "g_variants", "GenomicVariant", null);

  EntryType(
      String id, String name, String partOfSpecification, List<Concept> permittedSearchConcepts) {
    this.id = id;
    this.name = name;
    this.partOfSpecification = partOfSpecification;
    this.permittedSearchConcepts = permittedSearchConcepts;
  }

  public static EntryType findByName(String nameOther) {
    return Arrays.stream(values())
        .filter(entryType -> entryType.getName().equalsIgnoreCase(nameOther))
        .findFirst()
        .orElseThrow(() -> new MolgenisException("Invalid entry type: " + nameOther));
  }

  private final String id;
  private final String name;
  private final String partOfSpecification;
  private final List<Concept> permittedSearchConcepts;

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getPartOfSpecification() {
    return partOfSpecification;
  }

  public List<Concept> getPermittedSearchConcepts() {
    return permittedSearchConcepts;
  }
}
