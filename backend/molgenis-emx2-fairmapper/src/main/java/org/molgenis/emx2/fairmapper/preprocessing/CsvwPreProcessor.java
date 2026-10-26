package org.molgenis.emx2.fairmapper.preprocessing;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Finds the CSVW tables that belong to each dataset and links them together, so later steps can use
 * those tables to figure out the dataset's variables.
 */
public class CsvwPreProcessor implements RdfPreProcessor {

  private static final Logger logger = LoggerFactory.getLogger(CsvwPreProcessor.class);

  private static final String DELETE =
      """
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX healthdcatap: <http://healthdataportal.eu/ns/health#>
      PREFIX csvw: <http://www.w3.org/ns/csvw#>

      DELETE {
          ?dataset healthdcatap:hasVariables ?tableGroup
      }
      WHERE {
          ?dataset a dcat:Dataset .
          ?dataset healthdcatap:hasVariables ?tableGroup .
          FILTER NOT EXISTS { ?tableGroup a csvw:Table . }
      }
      """;

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
          ?dataset healthdcatap:hasVariables ?tableGroup .
          ?tableGroup a csvw:TableGroup .
          ?tableGroup csvw:table ?table .
          ?table a csvw:Table .
      }
      """;

  // Keep WHERE identical to DELETE!
  private static final String SELECT =
      """
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX healthdcatap: <http://healthdataportal.eu/ns/health#>
      PREFIX csvw: <http://www.w3.org/ns/csvw#>

      SELECT ?tableGroup
      WHERE {
          ?dataset a dcat:Dataset .
          ?dataset healthdcatap:hasVariables ?tableGroup .
          FILTER NOT EXISTS { ?tableGroup a csvw:Table . }
      }
      """;

  @Override
  public void process(Repository repository) {
    try (RepositoryConnection conn = repository.getConnection()) {
      GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, CONSTRUCT);
      Model result = QueryResults.asModel(graphQuery.evaluate());
      logger.info("linked {} table(s) to datasets", result.size());

      long nrTableGroups = getTableGroupCount(conn);
      conn.prepareUpdate(DELETE).execute();
      if (nrTableGroups > 0) {
        logger.info("Removed {} table group subjects that are linked to datasets", nrTableGroups);
      }

      conn.add(result);
      conn.commit();
    }
  }

  private static long getTableGroupCount(RepositoryConnection conn) {
    try (TupleQueryResult bindingSets = conn.prepareTupleQuery(SELECT).evaluate()) {
      return bindingSets.stream().count();
    }
  }
}
