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
public class CrawlingRdfExtractor implements RdfExtractor {

  private static final Logger logger = LoggerFactory.getLogger(CrawlingRdfExtractor.class);

  private final List<CrawlStep> crawlSteps;
  private final boolean strict;

  public CrawlingRdfExtractor() {
    this(CrawlSteps.FDP.steps(), false);
  }

  private CrawlingRdfExtractor(List<CrawlStep> crawlSteps, boolean strict) {
    this.crawlSteps = crawlSteps;
    this.strict = strict;
  }

  public CrawlingRdfExtractor withCrawlSteps(List<CrawlStep> crawlSteps) {
    return new CrawlingRdfExtractor(crawlSteps, strict);
  }

  public CrawlingRdfExtractor withStrict() {
    return new CrawlingRdfExtractor(crawlSteps, true);
  }

  @Override
  public void addRdfToRepository(Repository repository, URI rootToAdd) {
    logger.info("Crawling FAIR Data Point: {}", rootToAdd);
    fetch(repository, rootToAdd);

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

    results.stream()
        .map(iri -> URI.create(iri.stringValue()))
        .forEach(iri -> fetch(repository, iri));

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

  private void fetch(Repository repository, URI uri) {
    try (RepositoryConnection conn = repository.getConnection()) {
      logger.info("Extracting rdf from endpoint: {}", uri);
      conn.add(uri.toURL());
    } catch (Exception e) {
      logger.error("Could not add RDF from {}", uri, e);
      if (strict) {
        throw new MolgenisException("Unable to add RDF from " + uri, e);
      }
    }
  }
}
