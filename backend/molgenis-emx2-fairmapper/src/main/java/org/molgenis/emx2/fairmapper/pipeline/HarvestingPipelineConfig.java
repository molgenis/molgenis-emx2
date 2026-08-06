package org.molgenis.emx2.fairmapper.pipeline;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.extractors.RdfExtractor;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;

public record HarvestingPipelineConfig(
    URI rdf,
    Schema schema,
    String[] tables,
    String outputPath,
    boolean loadDataEnabled,
    RdfExtractor extractor,
    RdfTransformer transformer,
    List<RdfPreProcessor> preProcessors,
    List<PostProcessor> postProcessors) {

  public boolean dumpEnabled() {
    return outputPath != null;
  }

  public static class Builder {

    private final URI rdf;
    private final Schema schema;
    private final RdfExtractor extractor;
    private final RdfTransformer transformer;

    private String[] tables = new String[0];
    private String outputPath = null;
    private boolean loadDataEnabled = false;

    private List<RdfPreProcessor> preProcessors = new ArrayList<>();
    private List<PostProcessor> postProcessors = new ArrayList<>();

    public Builder(URI rdf, Schema schema, RdfExtractor extractor, RdfTransformer transformer) {
      this.rdf = rdf;
      this.schema = schema;
      this.extractor = extractor;
      this.transformer = transformer;
    }

    public Builder setTables(String... tables) {
      this.tables = tables;
      return this;
    }

    public Builder withDumpEnabled(String outputPath) {
      this.outputPath = outputPath;
      return this;
    }

    public Builder withPreProcessors(RdfPreProcessor... preProcessors) {
      this.preProcessors = Arrays.asList(preProcessors);
      return this;
    }

    public Builder withPostProcessors(PostProcessor... postProcessors) {
      this.postProcessors = Arrays.asList(postProcessors);
      return this;
    }

    public Builder enableDataLoading() {
      this.loadDataEnabled = true;
      return this;
    }

    public HarvestingPipelineConfig build() {
      return new HarvestingPipelineConfig(
          rdf,
          schema,
          tables,
          outputPath,
          loadDataEnabled,
          extractor,
          transformer,
          preProcessors,
          postProcessors);
    }
  }
}
