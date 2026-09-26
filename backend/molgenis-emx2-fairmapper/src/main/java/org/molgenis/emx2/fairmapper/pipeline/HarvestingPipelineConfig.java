package org.molgenis.emx2.fairmapper.pipeline;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.SchemaMetadataProvider;
import org.molgenis.emx2.fairmapper.extractors.RdfExtractor;
import org.molgenis.emx2.fairmapper.load.DataLoader;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;

public record HarvestingPipelineConfig(
    URI rdf,
    List<String> tables,
    String outputPath,
    SchemaMetadataProvider schemaMetadataProvider,
    String schemaName,
    RdfExtractor extractor,
    RdfTransformer transformer,
    List<RdfPreProcessor> preProcessors,
    List<PostProcessor> postProcessors,
    DataLoader dataLoader) {

  public boolean dumpEnabled() {
    return outputPath != null;
  }

  public boolean loadEnabled() {
    return dataLoader != null;
  }

  public static class Builder {

    private final URI rdf;
    private final RdfExtractor extractor;
    private final RdfTransformer transformer;
    private final SchemaMetadataProvider schemaMetadataProvider;
    private final String schemaName;
    private DataLoader dataLoader = null;

    private List<String> tables = new ArrayList<>();
    private String outputPath = null;

    private List<RdfPreProcessor> preProcessors = new ArrayList<>();
    private List<PostProcessor> postProcessors = new ArrayList<>();

    public Builder(
        URI rdf,
        String schemaName,
        SchemaMetadataProvider schemaMetadataProvider,
        RdfExtractor extractor,
        RdfTransformer transformer) {
      this.rdf = rdf;
      this.schemaName = schemaName;
      this.schemaMetadataProvider = schemaMetadataProvider;
      this.extractor = extractor;
      this.transformer = transformer;
    }

    public Builder setTables(String... tables) {
      this.tables = List.of(tables);
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

    public Builder withDataLoader(DataLoader dataLoader) {
      this.dataLoader = dataLoader;
      return this;
    }

    public HarvestingPipelineConfig build() {
      return new HarvestingPipelineConfig(
          rdf,
          tables,
          outputPath,
          schemaMetadataProvider,
          schemaName,
          extractor,
          transformer,
          preProcessors,
          postProcessors,
          dataLoader);
    }
  }
}
