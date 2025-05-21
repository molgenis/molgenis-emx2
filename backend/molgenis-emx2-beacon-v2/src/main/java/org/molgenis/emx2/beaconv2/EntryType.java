package org.molgenis.emx2.beaconv2;

import static org.molgenis.emx2.beaconv2.BeaconSpec.*;
import static org.molgenis.emx2.beaconv2.filter.FilterConceptVP.*;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.beaconv2.filter.FilterConceptVP;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum EntryType {
  INDIVIDUALS(
      "Individuals",
      "individuals",
      "Individual",
      "NCIT:C25190",
      "Person",
      List.of(BEACON_V2, BEACON_VP),
      List.of(SEX, DISEASE, PHENOTYPE, CAUSAL_GENE, AGE_THIS_YEAR, AGE_OF_ONSET, AGE_AT_DIAG)),
  BIOSAMPLES(
      "Biosamples",
      "biosamples",
      "Biosample",
      "NCIT:C70699",
      "Biospecimen",
      List.of(BEACON_V2, BEACON_VP),
      List.of(SEX, DISEASE, AGE_THIS_YEAR, AGE_AT_DIAG, BIOSAMPLE_TYPE)),
  CATALOGS(
      "Dataset",
      "catalogs",
      "Catalog",
      "NCIT:C47824",
      "Data set",
      List.of(BEACON_VP),
      List.of(DISEASE, PHENOTYPE, RESOURCE_TYPE)),
  GENOMIC_VARIANT(
      "GenomicVariants",
      "g_variants",
      "GenomicVariant",
      "ENSGLOSSARY:0000092",
      "Variant",
      List.of(BEACON_V2)),
  DATASETS("Dataset", "datasets", "Dataset", "NCIT:C47824", "Data set", List.of(BEACON_V2)),
  ANALYSES(
      "IndividualAnalyses",
      "analyses",
      "Analysis",
      "edam:operation_2945",
      "Analysis",
      List.of(BEACON_V2)),
  COHORTS("Cohorts", "cohorts", "Cohort", "NCIT:C61512", "Cohort", List.of(BEACON_V2)),
  RUNS("SequencingRuns", "runs", "Run", "NCIT:C148088", "Sequencing run", List.of(BEACON_V2));

  EntryType(
      String id,
      String name,
      String singular,
      String ontologyTerm,
      String ontologyLabel,
      List<BeaconSpec> spec,
      List<FilterConceptVP> filters) {
    this.id = id;
    this.name = name;
    this.singular = singular;
    this.ontologyTerm = ontologyTerm;
    this.ontologyLabel = ontologyLabel;
    this.partOfSpecification = spec;
    this.permittedFilters = filters;
  }

  EntryType(
      String id, String name, String singular, String term, String label, List<BeaconSpec> spec) {
    this(id, name, singular, term, label, spec, null);
  }

  public static EntryType findByName(String nameOther) {
    return Arrays.stream(values())
        .filter(entryType -> entryType.getName().equalsIgnoreCase(nameOther))
        .findFirst()
        .orElseThrow(() -> new MolgenisException("Invalid entry type: " + nameOther));
  }

  private final String id;
  private final String name;
  private final String singular;
  private final String ontologyTerm;
  private final String ontologyLabel;
  private final List<BeaconSpec> partOfSpecification;
  private final List<FilterConceptVP> permittedFilters;

  public static List<EntryType> getEntryTypesOfSpec(BeaconSpec spec) {
    return Arrays.stream(values())
        .filter(entryType -> entryType.partOfSpecification.contains(spec))
        .toList();
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getSingular() {
    return singular;
  }

  public List<FilterConceptVP> getPermittedFilters() {
    return permittedFilters;
  }

  public boolean validateSpecification(BeaconSpec otherSpec) {
    return this.partOfSpecification.contains(otherSpec);
  }

  public String getOntologyTerm() {
    return ontologyTerm;
  }

  public String getOntologyLabel() {
    return ontologyLabel;
  }
}
