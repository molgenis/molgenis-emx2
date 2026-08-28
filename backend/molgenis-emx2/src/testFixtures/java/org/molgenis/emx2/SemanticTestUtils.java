package org.molgenis.emx2;

import org.eclipse.rdf4j.model.IRI;

public class SemanticTestUtils {
  public static String toSemantic(IRI iri) {
    return "<%s>".formatted(iri.stringValue());
  }
}
