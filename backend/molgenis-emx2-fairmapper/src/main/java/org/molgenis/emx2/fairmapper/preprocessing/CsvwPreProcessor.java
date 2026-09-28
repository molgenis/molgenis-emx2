package org.molgenis.emx2.fairmapper.preprocessing;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.molgenis.emx2.MolgenisException;
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
          ?tableGroup a csvw:TableGroup .
          ?tableGroup csvw:table ?table .
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
      }
      """;

  private static final String SELECT =
      """
      PREFIX dcat: <http://www.w3.org/ns/dcat#>
      PREFIX healthdcatap: <http://healthdataportal.eu/ns/health#>
      PREFIX csvw: <http://www.w3.org/ns/csvw#>

      SELECT ?tableGroup
      WHERE {
          ?dataset a dcat:Dataset .
          ?dataset healthdcatap:hasVariables ?tableGroup .
          ?tableGroup a csvw:TableGroup .
          ?tableGroup csvw:table ?table .
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

      long nrTableGroups = getTableGroupCount(conn);
      if (nrTableGroups > 0) {
        logger.info(
            "Found {} table group subjects that are linked to datasets, will try to remove them",
            nrTableGroups);
        conn.prepareUpdate(DELETE).execute();
        nrTableGroups = getTableGroupCount(conn);
        logger.info("Removed table group subjects from datasets, {} are left", nrTableGroups);
        if (nrTableGroups > 0) {
          throw new MolgenisException(
              "Unable to remove all table groups that are linked to datasets");
        }
      }
    }
  }

  private static long getTableGroupCount(RepositoryConnection conn) {
    return conn.prepareTupleQuery(SELECT).evaluate().stream().count();
  }
}
