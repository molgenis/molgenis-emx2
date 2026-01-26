package org.molgenis.emx2.fairmapper.model.step;

import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public record MutateStep(String path) implements StepConfig {
  @Override
  public List<TestCase> tests() {
    return List.of();
  }
}
