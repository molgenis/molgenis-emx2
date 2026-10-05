package org.molgenis.emx2.fairmapper.pipeline.definition;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import java.util.List;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.extractors.CrawlStep;

/**
 * Pipeline file entry for the Extract stage.
 *
 * <pre>
 * - extract:
 *     url: https://fdp.example.org
 *     crawl: fdp        # optional; a crawl preset or a list of crawl steps
 *     strict: false     # optional, defaults to true
 * </pre>
 *
 * <p>Without {@code crawl}, only the document at {@code url} is fetched.
 *
 * @param url where to fetch the source RDF from
 * @param crawl crawl steps to follow from {@code url}, in order
 * @param strict whether a resource that can't be fetched aborts the harvest
 */
public record ExtractSpec(
    URI url,
    @JsonDeserialize(using = CrawlStepsDeserializer.class) List<CrawlStep> crawl,
    Boolean strict) {

  public ExtractSpec {
    if (crawl == null) {
      crawl = List.of();
    }
    if (strict == null) {
      strict = true;
    }
  }

  void validate() {
    if (url == null) {
      throw new MolgenisException("Invalid pipeline file: extract requires url");
    }
    if (!url.isAbsolute()) {
      throw new MolgenisException("Invalid pipeline file: extract url must be absolute: " + url);
    }
  }
}
