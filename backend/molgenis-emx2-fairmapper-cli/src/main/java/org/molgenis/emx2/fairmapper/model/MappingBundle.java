package org.molgenis.emx2.fairmapper.model;

import java.util.List;
import org.molgenis.emx2.fairmapper.FairMapperException;
import org.molgenis.emx2.fairmapper.model.step.QueryStep;
import org.molgenis.emx2.fairmapper.model.step.StepConfig;
import org.molgenis.emx2.fairmapper.model.step.TransformStep;

public record MappingBundle(
    String name, String version, List<Mapping> mappings, List<Endpoint> endpoints) {

  public List<Mapping> getMappings() {
    if (mappings != null && !mappings.isEmpty()) return mappings;
    if (endpoints != null) {
      return endpoints.stream()
          .map(e -> new Mapping(null, e.path(), e.methods(), convertSteps(e.steps()), e.e2e()))
          .toList();
    }
    return List.of();
  }

  private static List<StepConfig> convertSteps(List<Step> oldSteps) {
    if (oldSteps == null) return List.of();
    return oldSteps.stream()
        .map(
            s -> {
              if (s.transform() != null) {
                return new TransformStep(s.transform(), s.tests());
              } else if (s.query() != null) {
                return new QueryStep(s.query(), s.tests());
              }
              throw new FairMapperException("Step must have either transform or query defined");
            })
        .map(step -> (StepConfig) step)
        .toList();
  }
}
