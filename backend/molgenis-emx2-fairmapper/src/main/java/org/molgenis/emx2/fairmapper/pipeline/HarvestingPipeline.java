package org.molgenis.emx2.fairmapper.pipeline;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.molgenis.emx2.MolgenisException;
import org.molgenis.emx2.fairmapper.postprocessing.PostProcessor;
import org.molgenis.emx2.fairmapper.preprocessing.RdfPreProcessor;
import org.molgenis.emx2.io.ImportSchemaTask;
import org.molgenis.emx2.io.tablestore.InMemoryTableStore;
import org.molgenis.emx2.io.tablestore.TableStore;
import org.molgenis.emx2.io.tablestore.TableStoreForCsvInZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HarvestingPipeline {

  private static final String OUTPUT_DIRECTORY_NAME = "fairmapper-output-";
  private static final Logger logger = LoggerFactory.getLogger(HarvestingPipeline.class);

  private final UUID harvestId = UUID.randomUUID();
  private final HarvestingPipelineConfig config;

  public HarvestingPipeline(HarvestingPipelineConfig config) {
    this.config = config;
  }

  public void execute() {
    logger.info("Starting harvesting pipeline");
    Repository repository = new SailRepository(new MemoryStore());

    if (config.dumpEnabled() && !outputDirectory().toFile().mkdirs()) {
      throw new MolgenisException("Could not create output directory: " + config.outputPath());
    }

    config.extractor().addRdfToRepository(repository, config.rdf());

    if (config.dumpEnabled()) {
      writeRepositoryToFile(repository, "extracted.ttl");
    }

    if (!config.preProcessors().isEmpty()) {
      preProcess(repository);
    }

    InMemoryTableStore transformed = transform(repository);

    if (!config.postProcessors().isEmpty()) {
      postProcess(transformed);
    }

    if (config.loadDataEnabled()) {
      load(transformed);
    }
  }

  private void preProcess(Repository extract) {
    for (RdfPreProcessor processor : config.preProcessors()) {
      processor.process(extract);
    }

    if (config.dumpEnabled()) {
      writeRepositoryToFile(extract, "preprocessed.ttl");
    }
  }

  private InMemoryTableStore transform(Repository extracted) {
    InMemoryTableStore transformed = config.transformer().transform(extracted);

    if (config.dumpEnabled()) {
      writeTableStoreToZip(transformed, config.tables(), "transformed.zip");
    }

    return transformed;
  }

  private void postProcess(InMemoryTableStore transform) {
    for (PostProcessor postProcessor : config.postProcessors()) {
      postProcessor.process(transform);
    }

    if (config.dumpEnabled()) {
      writeTableStoreToZip(transform, config.tables(), "postprocessed.zip");
    }
  }

  private void load(InMemoryTableStore tableStore) {
    ImportSchemaTask tasks =
        new ImportSchemaTask(tableStore, config.schema(), false, config.tables())
            .setFilter(ImportSchemaTask.Filter.DATA_ONLY);

    tasks.run();
    while (tasks.isRunning()) {
      logger.info("waiting...");
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new MolgenisException("Something went wrong when uploading the data: ", e);
      }
    }
  }

  private void writeTableStoreToZip(TableStore store, String[] tables, String filename) {
    TableStoreForCsvInZipFile zip =
        new TableStoreForCsvInZipFile(outputDirectory().resolve(filename));
    for (String tableName : tables) {
      List<String> columnNames =
          StreamSupport.stream(store.readTable(tableName).spliterator(), false)
              .flatMap(row -> row.getColumnNames().stream())
              .distinct()
              .toList();
      zip.writeTable(tableName, columnNames, store.readTable(tableName));
    }
  }

  private void writeRepositoryToFile(Repository extract, String filename) {
    try (FileOutputStream fos = new FileOutputStream(outputDirectory().resolve(filename).toFile());
        RepositoryConnection connection = extract.getConnection()) {
      RDFWriter writer = Rio.createWriter(RDFFormat.TURTLE, fos);
      writer.startRDF();
      for (Statement statement : connection.getStatements(null, null, null)) {
        writer.handleStatement(statement);
      }
      writer.endRDF();
    } catch (IOException e) {
      throw new MolgenisException("Unable to write extract results to file", e);
    }
  }

  private Path outputDirectory() {
    return Path.of(config.outputPath()).resolve(OUTPUT_DIRECTORY_NAME + harvestId);
  }
}
