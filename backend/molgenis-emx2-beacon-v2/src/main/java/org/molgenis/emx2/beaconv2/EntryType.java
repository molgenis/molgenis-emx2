package org.molgenis.emx2.beaconv2;

import org.molgenis.emx2.MolgenisException;

public enum EntryType {
  INDIVIDUALS("Individuals", "individuals", "Individual"),
  DATASETS("Dataset", "datasets", "Dataset"),
  ANALYSES("Analyses", "analyses", "Analysis"),
  COHORTS("Cohorts", "cohorts", "Cohort"),
  BIOSAMPLES("Biosamples", "biosamples", "Biosample"),
  RUNS("Runs", "runs", "Run"),
  GENOMIC_VARIANT("GenomicVariations", "g_variants", "GenomicVariant");

  EntryType(String id, String name, String partOfSpecification) {
    this.id = id;
    this.name = name;
    this.partOfSpecification = partOfSpecification;
  }

  public static EntryType findByName(String name) {
    EntryType result = null;
    for (EntryType entryType : values()) {
      if (entryType.getName().equalsIgnoreCase(name)) {
        result = entryType;
        break;
      }
    }
    if (result == null) throw new MolgenisException("Invalid entry type: %s".formatted(name));

    return result;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getPartOfSpecification() {
    return partOfSpecification;
  }

  private String id;
  private String name;
  private String partOfSpecification;
}
