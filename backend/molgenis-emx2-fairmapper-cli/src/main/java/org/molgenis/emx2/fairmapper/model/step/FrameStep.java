package org.molgenis.emx2.fairmapper.model.step;

import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public record FrameStep(String path, Boolean unmapped, List<TestCase> tests)
    implements StepConfig {}
