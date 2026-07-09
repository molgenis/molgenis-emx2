package org.molgenis.emx2.fairmapper.preprocessing;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Normalises abbreviated age predicates on {@code dcat:Dataset} resources to their canonical
 * Health-DCAT-AP equivalents.
 *
 * <p>Some sources emit {@code healthdcatap:minTypicalAge} and {@code healthdcatap:maxTypicalAge}
 * instead of the canonical {@code healthdcatap:minimumTypicalAge} and {@code
 * healthdcatap:maximumTypicalAge}. This pre-processor reads the abbreviated forms and writes the
 * canonical ones, so downstream SPARQL queries only need to handle a single predicate name.
 *
 * <p>Each age is handled independently: a resource that carries only one of the two abbreviated
 * predicates will receive only the corresponding canonical predicate.
 */
public class TypicalAgeRdfPreProcessor implements RdfPreProcessor {

  private static final String CONSTRUCT =
      """
      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
      PREFIX healthdcatap: <http://healthdataportal.eu/ns/health#>

      CONSTRUCT {
          ?Resources healthdcatap:maximumTypicalAge ?maxAge .
          ?Resources healthdcatap:minimumTypicalAge ?minAge .
      }
      WHERE {
          ?Resources rdf:type <%s> .
          OPTIONAL { ?Resources healthdcatap:maxTypicalAge ?maxAge . }
          OPTIONAL { ?Resources healthdcatap:minTypicalAge ?minAge . }
      }
      """;

  @Override
  public void process(SailRepository repository) {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      constructForIri(conn, DCAT.DATASET);
      conn.commit();
    }
  }

  private static void constructForIri(SailRepositoryConnection conn, IRI iri) {
    GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTRUCT.formatted(iri));
    Model result = QueryResults.asModel(graphQuery.evaluate());
    result.forEach(conn::add);
  }
}
