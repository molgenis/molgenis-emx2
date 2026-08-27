package org.molgenis.emx2.fairmapper.pipeline;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.molgenis.emx2.Database;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.Schema;
import org.molgenis.emx2.fairmapper.extractors.FdpRdfExtractor;
import org.molgenis.emx2.fairmapper.extractors.RdfExtractor;
import org.molgenis.emx2.fairmapper.extractors.RemoteRdfExtractor;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.schemas.DatabaseSchemaFetcher;
import org.molgenis.emx2.fairmapper.schemas.GraphqlSchemaFetcher;
import org.molgenis.emx2.fairmapper.schemas.SchemaFetcher;
import org.molgenis.emx2.fairmapper.tasks.DataLoader;
import org.molgenis.emx2.fairmapper.tasks.DatabaseDataLoader;
import org.molgenis.emx2.fairmapper.tasks.RemoteDataLoader;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;
import org.molgenis.emx2.fairmapper.transform.SparqlSelectRdfTransformer;
import org.molgenis.emx2.graphql.GraphqlClient;
import org.molgenis.emx2.rdf.generators.query.TableQueryGenerator;

public record HarvestingPipelineConfig(
    URI rdf,
    List<String> tables,
    String outputPath,
    RdfExtractor extractor,
    RdfTransformer transformer,
    SchemaFetcher schemaFetcher,
    String schemaName,
    List<RdfPreProcessor> preProcessors,
    List<PostProcessor> postProcessors,
    DataLoader loader) {

  public boolean dumpEnabled() {
    return outputPath != null;
  }

  public boolean loadEnabled() {
    return loader != null;
  }

  public static class Builder {

    private final URI rdf;
    private final RdfExtractor extractor;
    private final RdfTransformer transformer;
    private final SchemaFetcher schemaFetcher;
    private final String schemaName;

    private List<String> tables = new ArrayList<>();
    private String outputPath = null;
    private DataLoader loader = null;

    private List<RdfPreProcessor> preProcessors = new ArrayList<>();
    private List<PostProcessor> postProcessors = new ArrayList<>();

    public Builder(
        URI rdf,
        RdfExtractor extractor,
        RdfTransformer transformer,
        SchemaFetcher schemaFetcher,
        String schemaName) {
      this.rdf = rdf;
      this.extractor = extractor;
      this.transformer = transformer;
      this.schemaFetcher = schemaFetcher;
      this.schemaName = schemaName;
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

    public Builder withLoader(DataLoader loader) {
      this.loader = loader;
      return this;
    }

    public HarvestingPipelineConfig build() {
      return new HarvestingPipelineConfig(
          rdf,
          tables,
          outputPath,
          extractor,
          transformer,
          schemaFetcher,
          schemaName,
          preProcessors,
          postProcessors,
          loader);
    }

    /**
     * Sets up a pipeline that reads the target schema straight from a locally running database and
     * loads harvested data into that same database.
     */
    public static Builder localConfig(
        Database database, URI rdf, String schemaName, String... tables) {
      Schema schema = database.getSchema(schemaName);
      if (schema == null) {
        throw new MolgenisException("Schema not found: " + schemaName);
      }

      return new Builder(
              rdf,
              defaultExtractor(rdf),
              defaultTransformer(),
              new DatabaseSchemaFetcher(database),
              schemaName)
          .setTables(tables)
          .withLoader(new DatabaseDataLoader(schema, tables));
    }

    /**
     * Sets up a pipeline that reads the target schema from, and loads harvested data into, a remote
     * emx2 instance over HTTP.
     */
    public static Builder remoteConfig(
        String endpoint, String token, URI rdf, String schemaName, String... tables) {
      GraphqlClient client = new GraphqlClient(endpoint, token);

      return new Builder(
              rdf,
              defaultExtractor(rdf),
              defaultTransformer(),
              new GraphqlSchemaFetcher(client),
              schemaName)
          .setTables(tables)
          .withLoader(newRemoteDataLoader(endpoint, token, schemaName, tables));
    }

    private static RdfTransformer defaultTransformer() {
      return new SparqlSelectRdfTransformer(new TableQueryGenerator());
    }

    private static RdfExtractor defaultExtractor(URI rdf) {
      return new FdpRdfExtractor(new RemoteRdfExtractor(), rdf);
    }

    private static DataLoader newRemoteDataLoader(
        String endpoint, String token, String schemaName, String... tables) {
      try {
        return new RemoteDataLoader(endpoint, token, schemaName, tables);
      } catch (MalformedURLException e) {
        throw new MolgenisException("Invalid remote endpoint: " + endpoint, e);
      }
    }
  }
}
