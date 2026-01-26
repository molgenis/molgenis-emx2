package org.molgenis.emx2.fairmapper.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.rdf4j.model.Model;
import org.molgenis.emx2.fairmapper.model.step.FetchStep;
import org.molgenis.emx2.fairmapper.rdf.FrameAnalyzer;
import org.molgenis.emx2.fairmapper.rdf.FrameDrivenFetcher;
import org.molgenis.emx2.fairmapper.rdf.JsonLdFramer;
import org.molgenis.emx2.fairmapper.rdf.RdfSource;
import org.molgenis.emx2.fairmapper.rdf.RdfToJsonLd;

public class FetchExecutor {
  private final RdfSource source;
  private final FrameAnalyzer analyzer;
  private final JsonLdFramer framer;
  private final ObjectMapper objectMapper;
  private final Path bundleDir;

  public FetchExecutor(RdfSource source, Path bundleDir) {
    this.source = source;
    this.analyzer = new FrameAnalyzer();
    this.framer = new JsonLdFramer();
    this.objectMapper = new ObjectMapper();
    this.bundleDir = bundleDir;
  }

  public JsonNode execute(FetchStep step, String url) throws IOException {
    JsonNode frame = null;
    if (step.frame() != null) {
      Path framePath = bundleDir.resolve(step.frame());
      frame = objectMapper.readTree(Files.readString(framePath));
    }

    Model model;
    if (frame != null) {
      FrameDrivenFetcher fetcher = new FrameDrivenFetcher(source, analyzer);
      model = fetcher.fetch(url, frame, step.maxDepth(), step.maxCalls());
      return framer.frame(model, frame);
    } else {
      model = source.fetch(url);
      RdfToJsonLd converter = new RdfToJsonLd();
      return objectMapper.readTree(converter.convert(model));
    }
  }
}
