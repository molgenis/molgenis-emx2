package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.ArrayList;
import java.util.List;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record CafeVariomeQuery(
    Subject subject,
    List<HPO> hpo,
    List<ORDO> ordo,
    List<Gene> genes,
    Variant variant,
    Advanced advanced) {
  public CafeVariomeQuery() {
    this(
        new Subject(),
        new ArrayList<>(),
        new ArrayList<>(),
        null,
        null,
        new Advanced(Granularity.COUNT, null));
  }

  public record Subject(
      boolean affectedOnly,
      Range age,
      Range ageFirstSymptoms,
      Range ageFirstDiagnosis,
      String gender,
      FamilyType familyType) {
    public Subject() {
      this(false, null, null, null, null, null);
    }
  }

  public record FamilyType(boolean family, boolean singletons, boolean trios) {}

  public record HPO(List<String> terms, int similarity, int minimumMatch, boolean useOrphaNet) {}

  public record ORDO(List<String> terms, boolean useHPO) {}

  public record Gene(List<Allele> alleles) {
    public record Allele(String gene, List<String> alleles) {}
  }

  public record Variant(
      List<String> genes,
      List<String> reactome,
      List<String> mutation,
      double maxAf,
      boolean useLocalAf) {}

  public record Advanced(Granularity granularity, RequiredFilters requiredFilters) {}

  public record RequiredFilters(
      boolean subject,
      boolean hpo,
      boolean ordo,
      boolean genes,
      boolean snomed,
      boolean variant,
      boolean source,
      boolean eav,
      SubjectCapability subjectCapability) {}

  public record SubjectCapability(
      boolean age, boolean gender, boolean familyType, boolean affected) {}
}
