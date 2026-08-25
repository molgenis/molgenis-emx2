package org.molgenis.emx2.fairmapper.extractors;

import java.util.List;
import org.eclipse.rdf4j.model.vocabulary.DCAT;

/**
 * The levels a FAIR Data Point exposes, in the order they have to be followed.
 *
 * <p>Note that RDF4J's {@code DCAT.DATASET} and {@code DCAT.DISTRIBUTION} are the classes {@code
 * dcat:Dataset} and {@code dcat:Distribution}; the linking properties are {@code HAS_DATASET} and
 * {@code HAS_DISTRIBUTION}.
 */
public final class FdpCrawlSteps {

  public static final CrawlStep CATALOGS = new CrawlStep("catalog", FDPO.METADATA_CATALOG);
  public static final CrawlStep DATASETS = new CrawlStep("dataset", DCAT.HAS_DATASET);
  public static final CrawlStep DISTRIBUTIONS =
      new CrawlStep("distribution", DCAT.HAS_DISTRIBUTION);
  public static final CrawlStep CSVW = new CrawlStep("csvw", DCAT.DOWNLOAD_URL);

  public static final List<CrawlStep> DEFAULT = List.of(CATALOGS, DATASETS, DISTRIBUTIONS, CSVW);

  private FdpCrawlSteps() {}
}
