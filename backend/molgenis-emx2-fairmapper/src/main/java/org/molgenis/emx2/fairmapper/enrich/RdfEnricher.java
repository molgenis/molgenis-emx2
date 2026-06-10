package org.molgenis.emx2.fairmapper.enrich;

import org.eclipse.rdf4j.repository.sail.SailRepository;

/**
 * Optional enrichment step in the DCAT harvesting pipeline, applied after RDF extraction and before
 * the SPARQL SELECT transformation.
 *
 * <p>Implementations mutate the repository by adding triples that can be derived from the existing
 * graph, making the data richer or more uniform for downstream queries.
 */
public interface RdfEnricher {

  /**
   * Adds derived triples to {@code repository}.
   *
   * <p>Implementations may read from and write to the same repository. All mutations must be
   * committed before this method returns so that subsequent pipeline steps see the enriched graph.
   */
  void enrich(SailRepository repository);
}
