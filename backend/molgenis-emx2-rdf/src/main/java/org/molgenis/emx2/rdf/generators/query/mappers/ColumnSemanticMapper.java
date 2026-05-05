package org.molgenis.emx2.rdf.generators.query.mappers;

import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;

public class ColumnSemanticMapper {

  private ColumnSemanticMapper() {
    throw new IllegalStateException("Utility class");
  }

  public static RdfPredicate resolveIri(String value) {
    if (value.startsWith("http://") || value.startsWith("https://")) {
      return Rdf.iri(value);
    }
    return () -> value;
  }
}
