package org.molgenis.emx2;

import org.eclipse.rdf4j.model.IRI;

public final class SemanticTestUtils {
  private SemanticTestUtils() {
    // no instances
  }

  public static String toSemantic(IRI iri) {
    return "<%s>".formatted(iri.stringValue());
  }
}
