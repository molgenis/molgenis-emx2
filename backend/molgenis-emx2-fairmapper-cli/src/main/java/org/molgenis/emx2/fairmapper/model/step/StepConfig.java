package org.molgenis.emx2.fairmapper.model.step;

import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public sealed interface StepConfig
    permits TransformStep, QueryStep, MutateStep, SqlQueryStep, FrameStep {
  List<TestCase> tests();
}
