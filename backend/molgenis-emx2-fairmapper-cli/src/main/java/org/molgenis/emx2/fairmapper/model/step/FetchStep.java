package org.molgenis.emx2.fairmapper.model.step;

import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public record FetchStep(
    String url,
    String accept,
    String frame,
    Integer maxDepth,
    Integer maxCalls,
    List<TestCase> tests)
    implements StepConfig {

  public FetchStep {
    if (accept == null) accept = "text/turtle";
    if (maxDepth == null) maxDepth = 5;
    if (maxCalls == null) maxCalls = 50;
  }
}
