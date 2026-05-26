package org.molgenis.emx2.rdf.generators.query;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfPredicate;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.rdf.mappers.NamespaceMapper;

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
 *   <li>{@code "https://schema.org/name"} -> {@code <https://schema.org/name>}
 *   <li>{@code "schema:name"} -> {@code schema:name}
 * </ul>
 */
public class RdfPredicateResolver {

  private RdfPredicateResolver() {
    throw new IllegalStateException("Utility class");
  }

  public static RdfPredicate resolve(String value, NamespaceMapper namespaceMapper) {
    String namespace = getNamespace(value);
    boolean containsPrefix =
        namespaceMapper.getAllNamespaces().stream()
            .map(Namespace::getPrefix)
            .anyMatch(namespace::equals);

    return (containsPrefix) ? () -> value : Rdf.iri(value);
  }

  private static String getNamespace(String value) {
    IRI iri = getIriValue(value);
    String namespace = iri.getNamespace();
    namespace = namespace.substring(0, namespace.length() - 1);
    return namespace;
  }

  private static IRI getIriValue(String value) {
    try {
      return Values.iri(value);
    } catch (IllegalArgumentException e) {
      throw new MolgenisException("Invalid IRI provided: " + value, e);
    }
  }
}
