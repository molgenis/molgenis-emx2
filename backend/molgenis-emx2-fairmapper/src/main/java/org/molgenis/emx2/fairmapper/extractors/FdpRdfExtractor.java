package org.molgenis.emx2.fairmapper.extractors;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FdpRdfExtractor {

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

  public FdpRdfExtractor(RdfExtractor rdfExtractor) {
    this.rdfExtractor = rdfExtractor;
  }

  /**
   * Extracts RDF data from a FAIR Data Point by querying its metadata catalogs and, for each
   * catalog, its datasets, then fetching the RDF for each catalog and dataset into an in-memory
   * repository.
   *
   * @param endpoint the endpoint of the RDF endpoint
   * @param fdpIdentifier the URI identifying the FAIR Data Point to extract catalogs and datasets
   *     from
   * @return an in-memory {@link SailRepository} containing the RDF extracted from the FDP's
   *     catalogs and datasets
   */
  public SailRepository extract(URI endpoint, String fdpIdentifier) {
    SailRepository sail = new SailRepository(new MemoryStore());
    rdfExtractor.addRdfToRepository(sail, endpoint);

    List<String> catalogs = queryCatalogs(sail, fdpIdentifier);
    for (String catalog : catalogs) {
      rdfExtractor.addRdfToRepository(sail, catalog);
    }

    List<String> datasets = queryDatasets(sail, catalogs);
    for (String dataset : datasets) {
      rdfExtractor.addRdfToRepository(sail, dataset);
    }

    return sail;
  }

  private static List<String> queryDatasets(SailRepository sail, List<String> catalogs) {
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

  private List<String> queryCatalogs(Repository repository, String fdpIdentifier) {
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
