package org.molgenis.emx2.fairmapper.extractors;

import java.util.List;
import org.eclipse.rdf4j.model.vocabulary.DCAT;
import org.molgenis.emx2.rdf.vocabulary.FDPO;
import org.molgenis.emx2.rdf.vocabulary.HEALTHDCATAP;

public enum CrawlSteps {
  FDP(
      new CrawlStep("catalog", FDPO.METADATA_CATALOG),
      new CrawlStep("dataset", DCAT.HAS_DATASET),
      new CrawlStep("csvw", HEALTHDCATAP.HAS_VARIABLES));

  private final List<CrawlStep> steps;

  CrawlSteps(CrawlStep... steps) {
    this.steps = List.of(steps);
  }

  public List<CrawlStep> steps() {
    return steps;
  }
}
