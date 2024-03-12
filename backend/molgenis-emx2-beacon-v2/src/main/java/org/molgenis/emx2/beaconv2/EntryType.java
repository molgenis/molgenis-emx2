package org.molgenis.emx2.beaconv2;

public enum EntryType {
  INDIVIDUALS("Individuals", "individuals", "Individual"),
  DATASETS("Dataset", "datasets", "Dataset"),
  ANALYSES("Analyses", "analyses", "Analysis"),
  COHORTS("Cohorts", "cohorts", "Cohort"),
  BIOSAMPLES("Biosamples", "biosamples", "Biosample");

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
    return result;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  private String id;
  private String name;
  private String partOfSpecification;
}
