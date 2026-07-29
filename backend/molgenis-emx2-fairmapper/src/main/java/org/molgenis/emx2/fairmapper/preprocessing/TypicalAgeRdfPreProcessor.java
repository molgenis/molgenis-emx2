package org.molgenis.emx2.fairmapper.preprocessing;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

/**
 * Normalises abbreviated typical age predicates on {@code dcat:Dataset} resources to the expected
 * version in the EMX2 catalog model, which is not abbreviated.
 *
 * <p>According to the Health-DCAT-AP spec, datasets emit {@code healthdcatap:minTypicalAge} and
 * instead of {@code healthdcatap:minimumTypicalAge} and {@code healthdcatap:maximumTypicalAge} (see
 * the <a
 * href='https://healthdataeu.pages.code.europa.eu/healthdcat-ap/releases/release-7/#healthdcatapmaxTypicalAge'>docs</a>)
 * which is used in EMX2. This pre-processor reads the abbreviated forms and writes the fully
 * written ones, so downstream SPARQL queries only need to handle a single predicate name.
 *
 * <p>Each age is handled independently: a resource that carries only one of the two abbreviated
 * predicates will receive only the corresponding canonical predicate.
 */
public class TypicalAgeRdfPreProcessor implements RdfPreProcessor {

  private static final String CONSTRUCT =
      """
          PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
          PREFIX healthdcatap: <http://healthdataportal.eu/ns/health#>
          PREFIX dcat: <http://www.w3.org/ns/dcat#>

          CONSTRUCT {
              ?Resources healthdcatap:maximumTypicalAge ?maxAge .
              ?Resources healthdcatap:minimumTypicalAge ?minAge .
          }
          WHERE {
              ?Resources rdf:type dcat:Dataset .
              OPTIONAL { ?Resources healthdcatap:maxTypicalAge ?maxAge . }
              OPTIONAL { ?Resources healthdcatap:minTypicalAge ?minAge . }
          }
          """;

  @Override
  public void process(SailRepository repository) {
    try (SailRepositoryConnection conn = repository.getConnection()) {
      GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTRUCT);
      Model result = QueryResults.asModel(graphQuery.evaluate());
      result.forEach(conn::add);
      conn.commit();
    }
  }
}
