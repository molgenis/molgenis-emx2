package org.molgenis.emx2.harvester;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;
import org.molgenis.emx2.*;
import org.molgenis.emx2.harvester.util.HarvestingTestSchema;
import org.molgenis.emx2.io.readers.CsvTableWriter;
import org.molgenis.emx2.io.tablestore.TableStore;

class FileBasedCatalogueTransformerTest {

  @Test
  void shouldMapResources() throws IOException {
    SailRepository repository = setupRepository();
    SchemaMetadata schema = HarvestingTestSchema.create();
    TableMetadata resourcesTable = schema.getTableMetadata("Resources");

    FileBasedCatalogueTransformer transformer =
        new FileBasedCatalogueTransformer(repository, schema);
    TableStore transform = transformer.transform();

    List<String> columns =
        resourcesTable.getDownloadColumnNames().stream().map(Column::getName).toList();
    StringWriter writer = new StringWriter();

    CsvTableWriter.write(transform.readTable("Resources"), columns, writer, ',');

    System.out.println("--------- OUTPUT CSV ---------");
    System.out.println(writer);
    Files.write(Path.of("resources.csv"), writer.toString().getBytes());
  }

  private static SailRepository setupRepository() {
    List<String> files =
        Stream.of("data/Catalog.ttl", "data/Dataset.ttl", "data/Distribution.ttl")
            .map(FileBasedExtractor.class::getResource)
            .filter(Objects::nonNull)
            .map(URL::getPath)
            .toList();

    SailRepository repository = new SailRepository(new MemoryStore());
    FileBasedExtractor extractor = new FileBasedExtractor(repository, files);
    extractor.extract();

    new TemporalEnrichment(repository).enrich();
    return repository;
  }
}
