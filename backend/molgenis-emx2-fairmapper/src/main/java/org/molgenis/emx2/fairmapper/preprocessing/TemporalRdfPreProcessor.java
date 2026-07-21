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
 * Enriches a repository by extracting the start and end year from {@code dcterms:temporal}
 * intervals and writing them as {@code dcat:startDate} and {@code dcat:endDate} directly on the
 * resource.
 *
 * <p>Only {@code dcat:Dataset} and {@code dcat:Catalog} resources are enriched. The temporal
 * interval must have both {@code dcat:startDate} and {@code dcat:endDate} present; resources with
 * an incomplete or absent interval are left untouched.
 *
 * <p>The enrichment is idempotent: running it multiple times on the same repository produces the
 * same result.
 */
public class TemporalRdfPreProcessor implements RdfPreProcessor {

  private static final String CONSTRUCT =
      """
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX dcterms: <http://purl.org/dc/terms/>
      PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

      CONSTRUCT {
          ?Resources dcat:startDate ?startYear .
          ?Resources dcat:endDate ?endYear .
      }
      WHERE {
          ?Resources rdf:type <%s> .
          ?Resources dcterms:temporal ?temporal .
          OPTIONAL {
              ?temporal dcat:endDate ?endDate .
              BIND(YEAR(?endDate) AS ?endYear)
          }
          OPTIONAL {
              ?temporal dcat:startDate ?startDate .
              BIND(YEAR(?startDate) AS ?startYear)
          }
      }
      """;

  @Override
  public void process(SailRepository repository) {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      constructForIri(conn, DCAT.DATASET);
      constructForIri(conn, DCAT.CATALOG);
      conn.commit();
    }
  }

  private static void constructForIri(SailRepositoryConnection conn, IRI iri) {
    GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTRUCT.formatted(iri));
    Model result = QueryResults.asModel(graphQuery.evaluate());
    result.forEach(conn::add);
  }
}
