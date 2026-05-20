package org.molgenis.emx2.rdf.generators.query;

import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;

/**
 * Resolves semantic annotations on columns to their appropriate RDF predicate representation.
 *
 * <p>Column semantics can be either full IRIs (e.g. {@code "https://schema.org/name"}) or plain
 * values (e.g. prefixed names like {@code "schema:name"}). Full IRIs must be wrapped in angle
 * brackets in SPARQL and Turtle syntax, which {@link Rdf#iri(String)} handles. Plain values must be
 * emitted as-is, without angle brackets.
 *
 * <p>Example:
 *
 * <ul>
 *   <li>{@code "https://schema.org/name"} → {@code <https://schema.org/name>}
 *   <li>{@code "schema:name"} → {@code schema:name}
 * </ul>
 */
public class RdfPredicateResolver {

  private RdfPredicateResolver() {
    throw new IllegalStateException("Utility class");
  }

  public static RdfPredicate resolve(String value) {
    if (value.startsWith("http://") || value.startsWith("https://")) {
      return Rdf.iri(value);
    }
    return () -> value;
  }
}
