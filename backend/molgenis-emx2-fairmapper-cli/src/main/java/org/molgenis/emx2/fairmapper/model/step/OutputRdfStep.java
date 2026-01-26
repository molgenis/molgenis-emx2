package org.molgenis.emx2.fairmapper.model.step;

import java.util.List;
import org.molgenis.emx2.fairmapper.model.TestCase;

public record OutputRdfStep(String defaultFormat, List<TestCase> tests) implements StepConfig {

  public String resolveFormat(String acceptHeader) {
    if (acceptHeader == null || acceptHeader.isBlank()) {
      return defaultFormat;
    }
    String accept = acceptHeader.toLowerCase();
    if (accept.contains("text/turtle")) return "turtle";
    if (accept.contains("application/n-triples")) return "ntriples";
    if (accept.contains("application/ld+json")) return "jsonld";
    if (accept.contains("application/json")) return "jsonld";
    return defaultFormat;
  }
}
