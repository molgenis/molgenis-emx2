package org.molgenis.emx2.fairmapper.model.step;

import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public sealed interface StepConfig permits FetchStep, TransformStep, QueryStep, MutateStep {
  List<TestCase> tests();
}
