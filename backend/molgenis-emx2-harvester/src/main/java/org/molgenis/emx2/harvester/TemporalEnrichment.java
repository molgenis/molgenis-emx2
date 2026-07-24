package org.molgenis.emx2.harvester;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

public class TemporalEnrichment {

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
              ?Resources rdf:type %s .
              ?Resources dcterms:temporal ?temporal .
              ?temporal dcat:endDate ?endDate .
              ?temporal dcat:startDate ?startDate .
              BIND(YEAR(?startDate) AS ?startYear)
              BIND(YEAR(?endDate) AS ?endYear)
          }
          """;

  private final SailRepository repository;

  public TemporalEnrichment(SailRepository repository) {
    this.repository = repository;
  }

  public void enrich() {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      GraphQuery graphQuery =
          conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTRUCT.formatted("dcat:Dataset"));
      Model result = QueryResults.asModel(graphQuery.evaluate());
      result.forEach(conn::add);

      graphQuery =
          conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTRUCT.formatted("dcat:Catalog"));
      result = QueryResults.asModel(graphQuery.evaluate());
      result.forEach(conn::add);
      conn.commit();
    }
  }
}
