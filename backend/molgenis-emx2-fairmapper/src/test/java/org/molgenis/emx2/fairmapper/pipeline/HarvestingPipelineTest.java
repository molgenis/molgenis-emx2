package org.molgenis.emx2.fairmapper.pipeline;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.DCTERMS;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.emx2.*;
import org.molgenis.emx2.datamodels.util.CompareTools;
import org.molgenis.emx2.fairmapper.extractors.RdfExtractor;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.fairmapper.transform.RdfTransformer;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.molgenis.emx2.sql.TestDatabaseFactory;

class HarvestingPipelineTest {

  private static final URI FDP_URI = URI.create("https://example.com/fdp");
  private static final SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();
  private static final IRI TEST_SUBJECT = valueFactory.createIRI("https://example.com/test");

  @TempDir private static Path tempDir;
  private static Path outputDirectory;
  private static Schema schema;
  private static StaticRdfExtractor rdfExtractor;
  private static StaticRdfTransformer transformer;

  @BeforeAll
  static void runPipeline() {
    setupSchema();

    rdfExtractor = new StaticRdfExtractor();
    transformer = new StaticRdfTransformer();
    HarvestingPipelineConfig config =
        new HarvestingPipelineConfig.Builder(FDP_URI, schema, rdfExtractor, transformer)
            .withDumpEnabled(tempDir.toString())
            .setTables("names", "products")
            .withPreProcessors(new StaticPreProcessor())
            .withPostProcessors(new StaticPostProcessor())
            .enableDataLoading()
            .build();
    HarvestingPipeline pipeline = new HarvestingPipeline(config);
    pipeline.execute();

    File file = new File(config.outputPath());
    String[] listing = Objects.requireNonNull(file.list());
    assertEquals(1, listing.length);
    String output = listing[0];
    outputDirectory = file.toPath().resolve(output);
  }

  private static void setupSchema() {
    Database database = TestDatabaseFactory.getTestDatabase();
    schema = database.dropCreateSchema(HarvestingPipelineTest.class.getSimpleName());
    schema.create(
        TableMetadata.table(
            "products", Column.column("barcode").setPkey().setType(ColumnType.STRING)),
        TableMetadata.table("names", Column.column("name").setPkey().setType(ColumnType.STRING)));
  }

  @Test
  void shouldExtract() {
    assertFileContentMatches(
        "extracted.ttl",
        """
        <https://example.com/test> <http://purl.org/dc/terms/title> "Harvester" .
        """);
  }

  @Test
  void shouldPreProcess() {
    assertFileContentMatches(
        "preprocessed.ttl",
        """

            <https://example.com/test> <http://purl.org/dc/terms/title> "Harvester";
              <http://purl.org/dc/terms/description> "new description" .
            """);
  }

  @Test
  void shouldTransform() {
    TableStoreForCsvInZipFile zipStore =
        new TableStoreForCsvInZipFile(outputDirectory.resolve("transformed.zip"));

    List<Row> rows =
        StreamSupport.stream(zipStore.readTable("names").spliterator(), false).toList();
    CompareTools.assertEquals(rows, List.of(Row.row("name", "foo"), Row.row("name", "bar")));
  }

  @Test
  void shouldPostProcess() {
    TableStoreForCsvInZipFile zipStore =
        new TableStoreForCsvInZipFile(outputDirectory.resolve("postprocessed.zip"));
    List<Row> names =
        StreamSupport.stream(zipStore.readTable("names").spliterator(), false).toList();
    CompareTools.assertEquals(names, List.of(Row.row("name", "foo"), Row.row("name", "bar")));

    List<Row> products =
        StreamSupport.stream(zipStore.readTable("products").spliterator(), false).toList();
    CompareTools.assertEquals(
        products, List.of(Row.row("barcode", "123"), Row.row("barcode", "321")));
  }

  @Test
  void shouldLoadData() {
    List<Row> names = schema.getTable("names").retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
    CompareTools.assertEquals(names, List.of(Row.row("name", "foo"), Row.row("name", "bar")));
    List<Row> products = schema.getTable("products").retrieveRows(Query.Option.EXCLUDE_MG_COLUMNS);
    CompareTools.assertEquals(
        products, List.of(Row.row("barcode", "123"), Row.row("barcode", "321")));
  }

  @Test
  void shouldSkipDumpingWhenDisabled() {
    HarvestingPipelineConfig config =
        new HarvestingPipelineConfig.Builder(FDP_URI, schema, rdfExtractor, transformer)
            .setTables("names", "products")
            .withPreProcessors(new StaticPreProcessor())
            .withPostProcessors(new StaticPostProcessor())
            .build();
    HarvestingPipeline pipeline = new HarvestingPipeline(config);
    pipeline.execute();

    // Should only contain the output directory from the setup method
    assertEquals(1, Objects.requireNonNull(tempDir.toFile().list()).length);
  }

  @Test
  void shouldThrowBeforeExtractingWhenConfiguredTableDoesNotExistInSchema() {
    StaticRdfExtractor extractor = new StaticRdfExtractor();
    HarvestingPipelineConfig config =
        new HarvestingPipelineConfig.Builder(FDP_URI, schema, extractor, transformer)
            .setTables("names", "unknown-table")
            .build();
    HarvestingPipeline pipeline = new HarvestingPipeline(config);

    MolgenisException exception = assertThrows(MolgenisException.class, pipeline::execute);

    assertEquals(
        "Unknown table(s) configured: unknown-table for schema: " + schema.getName(),
        exception.getMessage());
  }

  private static void assertFileContentMatches(String fileName, String fileContent) {
    try {
      String extracted = Files.readString(outputDirectory.resolve(fileName));
      assertEquals(fileContent, extracted);
    } catch (IOException e) {
      fail("Unable to read file contents: " + fileName);
    }
  }

  private static class StaticRdfExtractor implements RdfExtractor {

    @Override
    public void addRdfToRepository(Repository repository, URI rootToAdd) {
      try (RepositoryConnection connection = repository.getConnection()) {
        connection.add(
            valueFactory.createStatement(
                TEST_SUBJECT, DCTERMS.TITLE, valueFactory.createLiteral("Harvester")));
      }
    }
  }

  private static class StaticRdfTransformer implements RdfTransformer {

    @Override
    public InMemoryTableStore transform(
        Repository repository, SchemaMetadata schema, List<String> tables) {
      InMemoryTableStore store = new InMemoryTableStore();
      store.writeTable(
          "names", List.of("name"), List.of(Row.row("name", "foo"), Row.row("name", "bar")));
      return store;
    }
  }

  private static class StaticPreProcessor implements RdfPreProcessor {

    @Override
    public void process(Repository repository) {
      try (RepositoryConnection connection = repository.getConnection()) {
        connection.add(
            valueFactory.createStatement(
                TEST_SUBJECT, DCTERMS.DESCRIPTION, valueFactory.createLiteral("new description")));
        connection.commit();
      }
    }
  }

  private static class StaticPostProcessor implements PostProcessor {

    @Override
    public void process(InMemoryTableStore tableStore) {
      tableStore.writeTable(
          "products",
          List.of("barcode"),
          List.of(Row.row("barcode", "123"), Row.row("barcode", "321")));
    }
  }
}
