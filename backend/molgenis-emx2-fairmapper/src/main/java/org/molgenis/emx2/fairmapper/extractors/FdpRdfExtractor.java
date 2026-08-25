package org.molgenis.emx2.fairmapper.extractors;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts RDF from a FAIR Data Point by walking its levels one at a time.
 *
 * <p>Each step queries the repository for the resources it links to and then fetches those into the
 * same repository, so that the next step has triples to query. Fetching and querying therefore have
 * to alternate; a single SPARQL property path would find nothing beyond the first level.
 */
public class FdpRdfExtractor implements RdfExtractor {

  private static final Logger logger = LoggerFactory.getLogger(FdpRdfExtractor.class);

  private final RdfExtractor rdfExtractor;
  private final List<CrawlStep> crawlSteps;

  public FdpRdfExtractor(RdfExtractor rdfExtractor) {
    this(rdfExtractor, FdpCrawlSteps.DEFAULT);
  }

  private FdpRdfExtractor(RdfExtractor rdfExtractor, List<CrawlStep> crawlSteps) {
    this.rdfExtractor = rdfExtractor;
    this.crawlSteps = crawlSteps;
  }

  public FdpRdfExtractor withCrawlSteps(CrawlStep... crawlSteps) {
    return new FdpRdfExtractor(rdfExtractor, List.of(crawlSteps));
  }

  @Override
  public void addRdfToRepository(Repository repository, URI rootToAdd) {
    IRI root = Values.iri(stripTrailingSlashes(rootToAdd));
    logger.info("Crawling FAIR Data Point: {}", root);
    rdfExtractor.addRdfToRepository(repository, root.stringValue());

    List<IRI> frontier = List.of(root);
    for (CrawlStep step : crawlSteps) {
      frontier = follow(repository, frontier, step);
    }
  }

  private List<IRI> follow(Repository repository, List<IRI> subjects, CrawlStep step) {
    List<IRI> found = objectsOf(repository, subjects, step.predicate());

    logger.info("Found {} {}(s): {}", found.size(), step.name(), found);
    if (found.isEmpty()) {
      logger.warn(
          "Crawl step '{}' found nothing for {} subject(s); check the step order and whether the"
              + " source uses {}",
          step.name(),
          subjects.size(),
          step.predicate());
    }

    found.forEach(iri -> fetchSkippingFailures(repository, iri));
    return found;
  }

  private static List<IRI> objectsOf(Repository repository, List<IRI> subjects, IRI predicate) {
    List<IRI> objects = new ArrayList<>();
    try (RepositoryConnection connection = repository.getConnection()) {
      for (IRI subject : subjects) {
        try (CloseableIteration<Statement> statements =
            connection.getStatements(subject, predicate, null)) {
          statements.stream()
              .map(Statement::getObject)
              .filter(IRI.class::isInstance)
              .map(IRI.class::cast)
              .forEach(objects::add);
        }
      }
    }

    return objects.stream().distinct().toList();
  }

  /**
   * A FAIR Data Point links to resources it does not control, so a download URL may well point at a
   * CSV or ZIP rather than at RDF. One unreadable resource should not end the harvest.
   */
  private void fetchSkippingFailures(Repository repository, IRI iri) {
    try {
      rdfExtractor.addRdfToRepository(repository, iri.stringValue());
    } catch (RuntimeException e) {
      logger.warn("Skipping {}: {}", iri, e.toString());
      logger.debug("Could not add RDF from {}", iri, e);
    }
  }

  private static String stripTrailingSlashes(URI uri) {
    return uri.toString().replaceAll("/+$", "");
  }
}
