package org.molgenis.emx2.fairmapper.extractors;

import org.eclipse.rdf4j.model.IRI;

/**
 * One level of a linked data crawl: follow {@code predicate} from every subject found by the
 * previous step. Steps form a chain, so their order has to match the structure of the source.
 *
 * @param name used in logging to identify the level
 * @param predicate links a subject to the resources fetched by this step
 */
public record CrawlStep(String name, IRI predicate) {}
