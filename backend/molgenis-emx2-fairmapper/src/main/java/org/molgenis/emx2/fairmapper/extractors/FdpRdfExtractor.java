package org.molgenis.emx2.fairmapper.extractors;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.molgenis.emx2.MolgenisException;
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
  private final boolean strict;

  public FdpRdfExtractor(RdfExtractor rdfExtractor) {
    this(rdfExtractor, FdpCrawlSteps.DEFAULT, false);
  }

  private FdpRdfExtractor(RdfExtractor rdfExtractor, List<CrawlStep> crawlSteps, boolean strict) {
    this.rdfExtractor = rdfExtractor;
    this.crawlSteps = crawlSteps;
    this.strict = strict;
  }

  public FdpRdfExtractor withCrawlSteps(CrawlStep... crawlSteps) {
    return new FdpRdfExtractor(rdfExtractor, List.of(crawlSteps), strict);
  }

  public FdpRdfExtractor withStrict() {
    return new FdpRdfExtractor(rdfExtractor, crawlSteps, true);
  }

  @Override
  public void addRdfToRepository(Repository repository, URI rootToAdd) {
    logger.info("Crawling FAIR Data Point: {}", rootToAdd);
    rdfExtractor.addRdfToRepository(repository, rootToAdd);

    Set<IRI> frontier = Set.of(Values.iri(rootToAdd.toString()));
    for (CrawlStep step : crawlSteps) {
      frontier = executeStep(repository, frontier, step);
    }
  }

  private Set<IRI> executeStep(Repository repository, Set<IRI> subjects, CrawlStep step) {
    Set<IRI> results = objectsOf(repository, subjects, step.predicate());

    logger.info("Found {} {}(s): {}", results.size(), step.name(), results);
    if (results.isEmpty()) {
      logger.warn(
          "Crawl step '{}' results nothing for {} subject(s); check the step order and whether the"
              + " source uses {}",
          step.name(),
          subjects.size(),
          step.predicate());
    }

    results.forEach(iri -> fetch(repository, iri));
    return results;
  }

  private static Set<IRI> objectsOf(Repository repository, Set<IRI> subjects, IRI predicate) {
    try (RepositoryConnection connection = repository.getConnection()) {
      return subjects.stream()
          .flatMap(subject -> connection.getStatements(subject, predicate, null).stream())
          .map(Statement::getObject)
          .filter(Value::isIRI)
          .map(IRI.class::cast)
          .collect(Collectors.toSet());
    }
  }

  private void fetch(Repository repository, IRI iri) {
    try {
      rdfExtractor.addRdfToRepository(repository, iri.stringValue());
    } catch (RuntimeException e) {
      logger.error("Could not add RDF from {}", iri, e);
      if (strict) {
        throw new MolgenisException("Unable to add RDF from " + iri, e);
      }
    }
  }
}
