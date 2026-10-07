package org.molgenis.emx2.fairmapper.extractors;

import java.util.List;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.molgenis.emx2.rdf.vocabulary.FDPO;

public enum CrawlSteps {
  FDP(
      new CrawlStep("catalog", FDPO.METADATA_CATALOG),
      new CrawlStep("dataset", DCAT.HAS_DATASET),
      new CrawlStep("distribution", DCAT.HAS_DISTRIBUTION),
      new CrawlStep("csvw", DCAT.DOWNLOAD_URL));

  private final List<CrawlStep> steps;

  CrawlSteps(CrawlStep... steps) {
    this.steps = List.of(steps);
  }

  public List<CrawlStep> steps() {
    return steps;
  }
}
