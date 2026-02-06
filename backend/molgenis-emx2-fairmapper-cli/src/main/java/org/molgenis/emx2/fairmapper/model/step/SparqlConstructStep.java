package org.molgenis.emx2.fairmapper.model.step;

import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public record SparqlConstructStep(String path, List<TestCase> tests) implements StepConfig {}
