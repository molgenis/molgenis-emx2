package org.molgenis.emx2.fairmapper.preprocessing;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the CSVW tables that belong to each dataset and links them together, so later steps can use
 * those tables to figure out the dataset's variables.
 */
public class StageCsvwPreProcessor implements RdfPreProcessor {

  private static final Logger logger = LoggerFactory.getLogger(StageCsvwPreProcessor.class);

  private static final String CONSTRUCT =
      """
          PREFIX dcat: <http://www.w3.org/ns/dcat#>
          PREFIX healthdcatap: <http://healthdataportal.eu/ns/health#>
          PREFIX csvw: <http://www.w3.org/ns/csvw#>
          CONSTRUCT {
              ?dataset healthdcatap:hasVariables ?table
          }
          WHERE {
              ?dataset a dcat:Dataset .
              ?dataset dcat:distribution ?distribution .
              ?distribution dcat:downloadURL ?csvw .
              ?csvw a csvw:TableGroup .
              ?csvw csvw:table ?table .
          }
          """;

  @Override
  public void process(Repository repository) {
    try (RepositoryConnection conn = repository.getConnection()) {
      GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTRUCT);
      Model result = QueryResults.asModel(graphQuery.evaluate());
      logger.info("linked {} table(s) to datasets", result.size());
      conn.add(result);
      conn.commit();
    }
  }
}
