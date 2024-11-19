package org.molgenis.emx2.cafevariome;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;
import org.molgenis.emx2.beaconv2.filter.Filter;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record CafeVariomeQuery(
    Subject subject,
    List<HPO> hpo,
    List<ORDO> ordo,
    List<Gene> genes,
    List<Variant> variant,
    Advanced advanced) {

  public record Subject(
      boolean affectedOnly,
      Range age,
      Range ageFirstSymptoms,
      Range ageFirstDiagnosis,
      String gender,
      FamilyType familyType) {}

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

  public record Advanced(Granularity granularity, List<Filter> requiredFilters) {}
}
