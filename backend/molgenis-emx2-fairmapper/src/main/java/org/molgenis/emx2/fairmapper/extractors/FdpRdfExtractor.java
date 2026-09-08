package org.molgenis.emx2.fairmapper.extractors;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FdpRdfExtractor implements RdfExtractor {

  private static final Logger logger = LoggerFactory.getLogger(FdpRdfExtractor.class);

  private static final String CATALOG_QUERY =
      """
      SELECT ?catalog
      WHERE {
        <%s> <https://w3id.org/fdp/fdp-o#metadataCatalog> ?catalog .
      }
      """;

  private static final String DATASET_QUERY =
      """
      PREFIX dcat: <http://www.w3.org/ns/dcat#>

      SELECT ?dataset
      WHERE {
        <%s> dcat:dataset ?dataset .
      }
      """;

  private final RdfExtractor rdfExtractor;
  private final URI endpoint;

  public FdpRdfExtractor(RdfExtractor rdfExtractor, URI endpoint) {
    this.rdfExtractor = rdfExtractor;
    this.endpoint = normalize(endpoint);
  }

  private static URI normalize(URI uri) {
    if (uri.toString().endsWith("/")) {
      return URI.create(uri.toString().replaceAll("/$", ""));
    }

    return uri;
  }

  public void addRdfToRepository(Repository repository) {
    addRdfToRepository(repository, endpoint);
  }

  /**
   * Extracts RDF data from a FAIR Data Point by querying its metadata catalogs and, for each
   * catalog, its datasets, then fetching the RDF for each catalog and dataset into the provided
   * repository.
   *
   * @param repository RDF repository that the query results are written to.
   * @param rootToAdd the URI identifying the FAIR Data Point to extract catalogs and datasets from
   */
  @Override
  public void addRdfToRepository(Repository repository, URI rootToAdd) {
    rdfExtractor.addRdfToRepository(repository, endpoint);

    List<String> catalogs = queryCatalogs(repository, rootToAdd);
    for (String catalog : catalogs) {
      rdfExtractor.addRdfToRepository(repository, catalog);
    }

    List<String> datasets = queryDatasets(repository, catalogs);
    for (String dataset : datasets) {
      rdfExtractor.addRdfToRepository(repository, dataset);
    }
  }

  private static List<String> queryDatasets(Repository sail, List<String> catalogs) {
    List<String> datasets = new ArrayList<>();
    try (RepositoryConnection connection = sail.getConnection()) {
      for (String catalog : catalogs) {
        logger.info("Querying for datasets in catalog: {}", catalog);
        TupleQuery tupleQuery = connection.prepareTupleQuery(DATASET_QUERY.formatted(catalog));
        try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
          List<String> results =
              evaluate.stream()
                  .map(result -> result.getValue("dataset"))
                  .map(Value::stringValue)
                  .toList();

          logger.info(
              "Found the following dataset{} for catalog {}: {}",
              datasets.size() == 1 ? "" : "s",
              catalog,
              results);
          results.stream().map(dataset -> "dataset : " + dataset).forEach(logger::info);
          datasets.addAll(results);
        }
      }
    }

    return datasets;
  }

  private List<String> queryCatalogs(Repository repository, URI fdpIdentifier) {
    logger.info("Querying for catalogs from FDP with identifier: {}", fdpIdentifier);
    try (RepositoryConnection connection = repository.getConnection()) {
      TupleQuery tupleQuery = connection.prepareTupleQuery(CATALOG_QUERY.formatted(fdpIdentifier));
      try (TupleQueryResult evaluate = tupleQuery.evaluate()) {
        List<String> catalogs =
            evaluate.stream()
                .map(result -> result.getValue("catalog"))
                .map(Value::stringValue)
                .toList();

        logger.info("Found {} catalog{}", catalogs.size(), catalogs.size() == 1 ? "" : "s");
        catalogs.stream().map(catalog -> "catalog : " + catalog).forEach(logger::info);
        return catalogs;
      }
    }
  }
}
