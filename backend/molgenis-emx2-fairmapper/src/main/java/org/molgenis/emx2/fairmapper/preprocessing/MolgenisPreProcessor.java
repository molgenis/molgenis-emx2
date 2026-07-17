package org.molgenis.emx2.fairmapper.preprocessing;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;

public class MolgenisPreProcessor implements RdfPreProcessor {

  private static final String DATASET_CONSTRUCT =
      """
          PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
          PREFIX dcterms: <http://purl.org/dc/terms/>
          PREFIX foaf: <http://xmlns.com/foaf/0.1/>
          PREFIX dcat: <http://www.w3.org/ns/dcat#>

          CONSTRUCT {
              ?Resources <https://molgenis.org/resources#id> ?title .
              ?Resources <https://molgenis.org/resources#type> "http://semanticscience.org/resource/SIO_001067" .
              ?Publisher <https://molgenis.org/organisations#id> ?title .
              ?Publisher <https://molgenis.org/organisations#resource> ?publisher_name .
          }
          WHERE {
              ?Resources rdf:type dcat:Dataset .
              ?Resources dcterms:publisher ?Publisher .
              ?Publisher foaf:name ?publisher_name .
              ?Resources dcterms:title ?title .
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
    GraphQuery graphQuery = conn.prepareGraphQuery(QueryLanguage.SPARQL, DATASET_CONSTRUCT);
    Model result = QueryResults.asModel(graphQuery.evaluate());
    result.forEach(conn::add);
  }
}
